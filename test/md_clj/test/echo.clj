(ns md-clj.test.echo
  (:use md-clj.broker
        md-clj.worker
        md-clj.client)
  (:use clojure.test
        re-rand)
  (:import org.zeromq.ZMsg))

(def ^:dynamic *verbose* false)

(defn random-strings-fixture
  [f]
  (def random-strings (repeatedly 1000 #(re-rand #"[A-Za-z0-9]{20}")))
  (f)
  ())

(use-fixtures :once random-strings-fixture)

;; start our broker in a future 
(future (start-broker "tcp://*:5555" *verbose*))

;; super simple echo test. the request data is copied verbatim back into the
;; the response, which is then tested.

(deftest echo-test
  (let [echo-worker (new-worker
                     "echo" "tcp://localhost:5555" *verbose*
                     (fn [request reply]
                       (doall (map #(.add reply %) (.toArray request)))))
        echo-client (new-client "tcp://localhost:5555" *verbose*)]
    
    (future (run echo-worker))
    (time
    (doseq [x random-strings]
      (let [request (ZMsg.)
            _ (.addString request x)
            reply (send* echo-client "echo" request)]
        ;;reply2 (send2 echo-client "echo" (list x))]
        
        (is (= x (-> (.toArray reply)
                           first
                           .getData
                           String.))))))))

;; test an async client. send a bunch of request. then go back and get
;; all the results
(deftest echo-async-test
  (let [echo-async-worker (new-worker
                           "echo-async" "tcp://localhost:5555" *verbose*
                           (fn [request reply]
                       (doall (map #(.add reply %)  (.toArray request)))))
        echo-async-client (new-client "tcp://localhost:5555" *verbose* true)]

  (future (run echo-async-worker))

  (doseq [x random-strings]
    (let [request (ZMsg.)
          _ (.addString request x)]
      (send* echo-async-client "echo-async" request)))

  (doseq [x random-strings]
    (let [reply (recv* echo-async-client)]
      (is (= x (-> (.toArray reply)
                   first
                   .getData
                   String.)))))))

(deftest echo-multi-test
  (let [echo-workers (repeat 10 
                       (new-worker
                        "echo-multi"
                        "tcp://localhost:5555" *verbose*
                        (fn [request reply]
                          ;;(Thread/sleep 500)
                          (doall (map #(.add reply %)
                                      (.toArray request))))))
        echo-client (new-client "tcp://localhost:5555" *verbose*)]

    (doseq [w echo-workers]
      (future (run w)))

    (time
    (doseq [x random-strings]
      (let [request (ZMsg.)
            _ (.addString request x)
            reply (send* echo-client "echo-multi" request)]
        (is (= x (-> (.toArray reply)
                           first
                           .getData
                           String.))))))))
