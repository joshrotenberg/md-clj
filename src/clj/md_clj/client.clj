(ns md-clj.client
  "Majordomo Client"
  (:import mdcliapi
           mdcliapi2
           [org.zeromq ZMsg ZFrame]))

(def ^:dynamic *client-debug* false)

(defrecord Client [^String endpoint ^Boolean verbose ^mdcliapi client async])

(defn new-client
  "Creates and returns a new client record."
  [endpoint & async]
  (if (first async)
    (Client. endpoint *client-debug* (mdcliapi2. endpoint *client-debug*) false)
    (Client. endpoint *client-debug* (mdcliapi. endpoint *client-debug*) false)))

(def new-client-memoize (memoize new-client))

(derive clojure.lang.PersistentVector ::collection)
(derive clojure.lang.PersistentList ::collection)

(defmulti send! (fn [client service request] (class request)))

(defmethod send! ZMsg [client service request]
  (prn client (name service) request)
  (.send (:client client) (name service) request))

(defmethod send! String [client service request]
  (let [r (ZMsg.)
        _ (.add r (ZFrame. request))]
    (.send (:client client) (name service) r)))

(defmethod send! (Class/forName "[B") [client service request]
  (let [r (ZMsg.)
        _ (.add r (ZFrame. request))]
    (.send (:client client) (name service) r)))

(defmethod send! ::collection [client service request]
  (let [r (ZMsg.)]
    (doseq [s request]
      (.add r (ZFrame. s)))
    (.send (:client client) (name service) r)))

(defmethod send! :default [client service request]
  (println "doesn't handle " (class request)))

(defn recv
  "Receive from an asynchronous request."
  [this]
  (.recv (:client this)))

(defn destroy
  "Destroy the Client."
  [this]
  (.destroy (:client this)))

(defmacro as-client
  [service endpoint & body]
  `(let [client# (new-client-memoize ~endpoint false)
         request# (do ~@body)
         response# (send! client# (name ~service) request#)]
     (if (> (.size response#) 1)
       (map #(.getData %) (.toArray response#))
       (.getData (.getFirst response#)))))

(defmacro as-client-async
  [service endpoint & body]
  `(let [client# (new-client-memoize ~endpoint true)
         request# (do ~@body)]
     (doseq [r# request#]
       (send! client# (name ~service) r#))
     (loop [col# [] n# 0]
       (if (<= (count request#) n#)
         col#
         (do
           (let [item# (recv client#)
                 res# (if (> (.size item#) 1)
                          (map #(.getData %) (.toArray item#))
                           (.getData (.getFirst item#)))]
             (recur (conj col# res#) (+ n# 1))))))))



         

