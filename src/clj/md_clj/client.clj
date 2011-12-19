(ns md-clj.client
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

(defn send*
  "Sends a request given a Client record, a service name an a ZMsg request."
  [^Client this ^String service ^ZMsg request]
  (.send (:client this) service request))

(defn send-strings
  "Sends a request given a Client record, a service name a sequence of strings."
  [^Client this ^String service strings]
  (let [request (ZMsg.)]
    (doseq [s strings]
      (.addString request s))
      (.send (:client this) service request)))

(defn recv*
  "Receive from an asynchronous request."
  [this]
  (.recv (:client this)))

(defn destroy*
  "Destroy the Client."
  [this]
  (.destroy (:client this)))



