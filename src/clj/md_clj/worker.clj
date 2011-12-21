(ns md-clj.worker
  (:import mdwrkapi
           [org.zeromq ZMsg ZFrame]))


(defrecord Worker [^String service ^String endpoint ^Boolean verbose function])

(defn new-worker [service endpoint verbose function]
  (Worker. service endpoint verbose function))

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

(defn run2
  [^Worker this]
  (let [worker (mdwrkapi. (:endpoint this)
                          (:service this)
                          (:verbose this))
        reply (ZMsg.)]
    (while (not (.isInterrupted (Thread/currentThread)))
      (let [request (.receive worker reply)
            s ((:function this) request)]
        (doall (map #(.add reply (ZFrame. %)) s))))))

 (defmacro as-worker
   [service endpoint & body]
   ;;`(let [worker# (new-worker (name ~service) ~endpoint false nil)
   `(let [worker# (mdwrkapi. ~endpoint (name ~service) false)
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
  
               
