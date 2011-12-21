(ns md-clj.client
  "Majordomo Client"
  (:import mdcliapi
           mdcliapi2
           org.zeromq.ZMsg))

(defrecord Client [^String endpoint ^Boolean verbose ^mdcliapi client async])

(defn new-client
  "Creates and returns a new client record."
  [endpoint verbose & async]
  (if (first async)
    (Client. endpoint verbose (mdcliapi2. endpoint verbose) false)
    (Client. endpoint verbose (mdcliapi. endpoint verbose) false)))

(def new-client-memoize (memoize new-client))

(defmulti send! (fn [client service request] (class request)))

(defmethod send! ZMsg [client service request]
  (.send (:client client) service request))

(defmethod send! String [client service request]
  (let [r (ZMsg.)
        _ (.addString r request)]
    (.send (:client client) service r)))

;; XXX: can these two functions be based on a superclass of PV and PL?
;; XXX: need to check that each element is a string and coerce if it isn't
(defmethod send! clojure.lang.PersistentVector [client service request]
  (let [r (ZMsg.)]
    (doseq [s request]
      (.addString r s))
    (.send (:client client) service r)))

(defmethod send! clojure.lang.PersistentList [client service request]
  (let [r (ZMsg.)]
    (doseq [s request]
      (.addString r s))
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


