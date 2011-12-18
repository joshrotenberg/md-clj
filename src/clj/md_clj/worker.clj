(ns md-clj.worker
  (:import mdwrkapi
           org.zeromq.ZMsg))

(defprotocol WorkerProtocol
  (run [this]))

(defrecord Worker [^String service ^String endpoint ^Boolean verbose function]
  WorkerProtocol
  (run [this]
    (let [worker (mdwrkapi. (:endpoint this)
                            (:service this)
                            (:verbose this))
          reply (ZMsg.)]
      (while (not (.isInterrupted (Thread/currentThread)))
        (let [request (.receive worker reply)]
          (do
            ((:function this) request reply)))))))
    
(defn new-worker [service endpoint verbose function]
  (Worker. service endpoint verbose function))