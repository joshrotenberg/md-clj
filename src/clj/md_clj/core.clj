(ns md-clj.core
  (:import [org.zeromq ZMsg ZFrame]))

(def ^:dynamic *debug* false)

(derive clojure.lang.PersistentVector ::collection)
(derive clojure.lang.PersistentList ::collection)
(derive (Class/forName "[Ljava.lang.String;") ::collection)
(derive (Class/forName "[[B") ::collection)

;; take various types and create a ZMsg for transport
(defmulti zmsg class)

(defmethod zmsg String [s]
  (doto (ZMsg.) (.add (ZFrame. s))))

(defmethod zmsg (Class/forName "[B") [b]
  (doto (ZMsg.) (.add (ZFrame. b))))

(defmethod zmsg ::collection [c]
  (let [m (ZMsg.)]
    (doseq [item c]
      (.add m (ZFrame. item)))
    m))

(defmethod zmsg java.lang.Number [n]
  (zmsg (str n)))

(defmethod zmsg (Class/forName "[Ljava.lang.Number;") [a]
  (let [m (ZMsg.)]
    (doseq [item a]
      (.add m (ZFrame. (str item))))
    m))

(defmethod zmsg :default [a]
  (println "doesn't handle " (class a) " yet"))

