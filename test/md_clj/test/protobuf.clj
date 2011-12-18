(ns bsa.test.md.protobuf
  (:use bsa.md.broker
        bsa.md.worker
        bsa.md.client)
  (:use clojure.test
        bsa.test.util
        protobuf)
  (:import org.zeromq.ZMsg))

(defprotobuf Request bsa.test.Test$Request)
(defprotobuf Response bsa.test.Test$Response)

(def ^:dynamic *verbose* false)

;; start our broker in a future 
(future (start-broker "tcp://*:5555" *verbose*))

;; test a request/response protocol buffer simulation. this is a more involved
;; test that uses a pair of protocol buffers to issue a request and then
;; return a reply based on the request.
(deftest protobuf-test
  (let [protobuf-worker (new-worker
                     "pb" "tcp://localhost:5555" *verbose*
                     (fn [request reply]
                       (let [request-pb
                             (protobuf-load Request
                                            (.getData (first (.toArray request))))
                             reply-pb (protobuf Response :ffuts
                                                (apply str (reverse (:stuff request-pb))) :c (apply + ((juxt :a :b) request-pb)))
                             _ (.addString reply (String. (protobuf-dump reply-pb)))]
                         reply)))
        protobuf-client (new-client "tcp://localhost:5555" *verbose*)]
    
  (future (run protobuf-worker))

  (dotimes [i 20]
    (let [request-pb (protobuf Request :stuff "foo" :a 20 :b 30)
          request (ZMsg.)
          _ (.addString request (String. (protobuf-dump request-pb)))
          reply (send* protobuf-client "pb" request)
          reply-pb (protobuf-load Response (.getData (first (.toArray reply))))]
      (is (= "oof" (:ffuts reply-pb)))
      (is (= 50 (:c reply-pb)))))))
    

