(ns md-clj.client
  "Majordomo Client"
  (:use md-clj.core)
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

(defn send!
  [client service request]
  (.send (:client client) (name service) (to-zmsg request)))

(defn recv
  "Receive from an asynchronous request."
  [this]
  (.recv (:client this)))

(defn destroy
  "Destroy the Client."
  [this]
  (.destroy (:client this)))

(defmacro as-client
  "Takes a service name keyword, an endpoint to connect to, and the body of
   a client method. The return value of the body code will be sent as the
   request, and the whole call will return the response."
  [service endpoint & body]
     `(let [client# (new-client-memoize ~endpoint false)
            request# (do ~@body)
            response# (send! client# (name ~service) request#)]
        (from-zmsg response# *return-type*)))

(defmacro as-client-async
  "Takes a service name keyword, an endpoint to connect to, and the body of
  a client method. The body should return a sequence of values, each of
  which will be a single asynchronous request sent to the worker. When the
  sequence has been exhausted, the results will then be fetched and returned as
  as a sequence in the same order as the request."
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
                 res# (from-zmsg item#)]
             (recur (conj col# res#) (+ n# 1))))))))



         

