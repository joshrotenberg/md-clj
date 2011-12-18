(ns bsa.test.md.big
  (:use bsa.md.broker
        bsa.md.worker
        bsa.md.client)
  (:use clojure.test
        bsa.test.util
        re-rand
        protobuf)
  (:import org.zeromq.ZMsg))

(def ^:dynamic *verbose* false)

;; start our broker in a future 
(future (start-broker "tcp://*:5555" *verbose*))

;; test lots of request/reply data. this test request a big hunk of data,
;; and then replies with another big hunk of data
(deftest big-test
  (let [data (apply str (repeat (* 1024 1024) "x"))
        big-worker (new-worker
                    "big" "tcp://localhost:5555" *verbose*
                    (fn [request reply]
                      (.addString reply data)))
        big-client (new-client "tcp://localhost:5555" *verbose*)]
    
    (future (run big-worker))

    (dotimes [i 100]
      (let [request (ZMsg.)
            _ (.addString request data)
            reply (send* big-client "big" request)]
        
        (is (= data  (-> (.toArray reply)
                         first
                         .getData
                         String.)))))))

