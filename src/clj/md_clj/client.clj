(ns md-clj.client
  "Majordomo Client"
  (:import mdcliapi
           mdcliapi2
           [org.zeromq ZMsg ZFrame]))

(defrecord Client [^String endpoint ^Boolean verbose ^mdcliapi client async])

(defn new-client
  "Creates and returns a new client record."
  [endpoint verbose & async]
  (if (first async)
    (Client. endpoint verbose (mdcliapi2. endpoint verbose) false)
    (Client. endpoint verbose (mdcliapi. endpoint verbose) false)))

(def new-client-memoize (memoize new-client))

(derive clojure.lang.PersistentVector ::collection)
(derive clojure.lang.PersistentList ::collection)

(defmulti send! (fn [client service request] (class request)))

(defmethod send! ZMsg [client service request]
  (.send (:client client) service request))

(defmethod send! String [client service request]
  (let [r (ZMsg.)
        _ (.add r (ZFrame. request))]
    (.send (:client client) service r)))

(defmethod send! (Class/forName "[B") [client service request]
  (let [r (ZMsg.)
        _ (.add r (ZFrame. request))]
    (.send (:client client) service r)))

(defmethod send! ::collection [client service request]
  (let [r (ZMsg.)]
    (doseq [s request]
      (.add r (ZFrame. s)))
    (.send (:client client) service r)))

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


