(ns md-clj.client.async
  (:import mdcliapi2
           org.zeromq.ZMsg))

(defprotocol AsyncClientProtocol
  (send* [this service request])
  (recv* [this])
  (destroy* [this]))

(defrecord AsyncClient [^String endpoint ^Boolean verbose ^mdcliapi2 client]
  AsyncClientProtocol
  (send* [this service request]
    (.send (:client this) service request))
  (recv* [this]
    (.recv (:client this)))
  (destroy* [this]
    (.destroy (:client this))))

(defn new-async-client
  [endpoint verbose]
  (AsyncClient. endpoint verbose (mdcliapi2. endpoint verbose)))

