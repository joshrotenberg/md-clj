(ns md-clj.core
  (:import [org.zeromq ZMsg ZFrame]))

(def ^:dynamic *debug* false)
(def ^{:dynamic true} *return-type* :as-bytes)

(derive clojure.lang.PersistentVector ::collection)
(derive clojure.lang.PersistentList ::collection)
(derive (Class/forName "[Ljava.lang.String;") ::collection)

;; XXX: we stringify everything in the collections above, but str on
;; a byte array isn't what we want, so handle [[B seperately
;;(derive (Class/forName "[[B") ::collection)

;; take various types and create a ZMsg for transport
(defmulti to-zmsg class)

(defmethod to-zmsg ZMsg [z] z)

(defmethod to-zmsg ZFrame [f]
  (doto (ZMsg.) (.add f)))

(defmethod to-zmsg (Class/forName "[Lorg.zeromq.ZFrame;") [a]
  (let [m (ZMsg.)]
    (doseq [item a]
      (.add m item))
    m))

(defmethod to-zmsg String [s]
  (to-zmsg (.getBytes s)))

(defmethod to-zmsg (Class/forName "[B") [b]
  (doto (ZMsg.) (.add (ZFrame. b))))

(defmethod to-zmsg ::collection [c]
  (let [m (ZMsg.)]
    (doseq [item c]
      (condp = (type item)
        java.lang.String (.add m (ZFrame. item))
        (Class/forName "[B") (.add m (ZFrame. item))
        (.add m (ZFrame. (str item))))) ;; stringify anything else
    m))

(defmethod to-zmsg (Class/forName "[[B") [c]
  (let [m (ZMsg.)]
    (doseq [item c]
      (.add m (ZFrame. item)))
    m))

(defmethod to-zmsg java.lang.Number [n]
  (to-zmsg (str n)))

(defmethod to-zmsg (Class/forName "[Ljava.lang.Number;") [a]
   (let [m (ZMsg.)]
     (doseq [item a]
       (.add m (ZFrame. (str item))))
     m))

(defmethod to-zmsg :default [a]
  (throw (Exception. (format "to-zmsg doesn't handle %s" (class a)))))

(defmacro with-message-type
  "Specify the type for the elements in a request to or response from
   a worker function if the default of byte array doesn't do it for
   you. Options are passed directly to from-zmsg:
  :as-string - all response items will be strings
  :as-number - all response items will be parsed as numbers
  :as-bytes - the default, all items will be byte arrays
  :as-zmsg - the unmolested raw ZMsg object"
  [return-type & body]
  `(binding [*return-type* ~return-type]
     ~@body))

(defn from-zmsg
  "Converts a ZMsg to a Clojure type. If the message has multiple frames,
   a collection is returned, otherwise a single value is returned. By default,
   returns items as a byte array. Specify :as-number to parse the result as a
   number (using read-string), :as-string to return a string, or :as-bytes
   to explicity use a byte array. :as-zmsg can be specified to get the raw
   ZMsg object itself."
  [z & [as]]
  (let [the-type (if (nil? as) *return-type* as)]
    (if (= the-type :as-zmsg)
      z
      (let [f (condp = the-type
                :as-number (fn [i] (read-string (.toString i)))
                :as-string (fn [s] (.toString s))
                :as-bytes (fn [b] (.getData b))
                (fn [b] (.getData b)))]
        (let [res (map f (.toArray z))]
          (if (> (count res) 1)
            res
            (first res)))))))


    
  