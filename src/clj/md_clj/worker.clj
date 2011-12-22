(ns md-clj.worker
  (:import mdwrkapi
           [org.zeromq ZMsg ZFrame]))

(def ^:dynamic *worker-debug* false)
(defrecord Worker [^String service ^String endpoint ^Boolean verbose function])

(defn new-worker
  [service endpoint function]
  (Worker. service endpoint *worker-debug* function))

(defn run
  "Run the worker."
  [^Worker this]
  (let [worker (mdwrkapi. (:endpoint this)
                          (:service this)
                          (:verbose this))
        reply (ZMsg.)]
    (while (not (.isInterrupted (Thread/currentThread)))
      (let [request (.receive worker reply)]
        (do
          ((:function this) request reply))))))

 (defmacro as-worker
   "Given a service name keyword, an endpoint, runs the body as the
   worker implementation. The request is magically available in the
   'request' var, and the body should return the response, if any."
   [service endpoint & body]
   `(let [worker# (mdwrkapi. ~endpoint (name ~service) *worker-debug*)
          reply# (ZMsg.)]
      (while (not (.isInterrupted (Thread/currentThread)))
        (let [requezt# (.receive worker# reply#)
              ~'request (if (> (.size requezt#) 1)
                          (map #(.getData %) (.toArray requezt#))
                          (.getData (.getFirst requezt#)))
              repli# (do ~@body)]
          (if (seq? repli#)
            (doall (map #(.add reply# (ZFrame. %)) repli#))
            (.add reply# (ZFrame. repli#)))))))
  