(ns md-clj.client
  (:import mdcliapi
           mdcliapi2
           org.zeromq.ZMsg))

(defprotocol ClientProtocol
  (send* [this service request])
  (recv* [this])
  (destroy* [this]))

(defrecord Client [^String endpoint ^Boolean verbose ^mdcliapi client async]
  ClientProtocol
  (send* [this service request]
    (.send (:client this) service request))
  (recv* [this]
    (.recv (:client this)))
  (destroy* [this]
    (.destroy (:client this))))

(defn new-client
  [endpoint verbose & async]
  (if (first async)
    (Client. endpoint verbose (mdcliapi2. endpoint verbose) false)
    (Client. endpoint verbose (mdcliapi. endpoint verbose) false)))


      