(ns md-clj.test.echo
  (:require [md-clj.broker :only [start-broker] :as mdb]
            [md-clj.worker :only [new-worker run] :as mdw]
            [md-clj.client :only [new-client send! recv] :as mdc])
  (:use clojure.test
        re-rand)
  (:import org.zeromq.ZMsg))

(defn random-strings-fixture
  [f]
  (def random-strings (repeatedly 1000 #(re-rand #"[A-Za-z0-9]{20}")))
  (f)
  ())

(use-fixtures :once random-strings-fixture)

;; start our broker in a future 
(future (mdb/start-broker "tcp://*:5555" false))

;; super simple echo test. the request data is copied verbatim back into the
;; the response, which is then tested.

(deftest echo-test
  (let [echo-worker (mdw/new-worker
                     "echo" "tcp://localhost:5555"
                     (fn [request reply]
                       (doall (map #(.add reply %) (.toArray request)))))
        echo-client (mdc/new-client "tcp://localhost:5555")]
    
    (future (mdw/run echo-worker))
    (time
     (doseq [x random-strings]
       (let [reply (mdc/send! echo-client "echo" x)]
         
        (is (= x (-> (.toArray reply)
                     first
                     .getData
                     String.))))))))

;; test an async client. send! a bunch of request. then go back and get
;; all the results
(deftest echo-async-test
  (let [echo-async-worker (mdw/new-worker
                           "echo-async" "tcp://localhost:5555"
                           (fn [request reply]
                       (doall (map #(.add reply %)  (.toArray request)))))
        echo-async-client (mdc/new-client "tcp://localhost:5555" true)]

  (future (mdw/run echo-async-worker))

  (doseq [x random-strings]
    (let [request (ZMsg.)
          _ (.addString request x)]
      (mdc/send! echo-async-client "echo-async" request)))

  (doseq [x random-strings]
    (let [reply (mdc/recv echo-async-client)]
      (is (= x (-> (.toArray reply)
                   first
                   .getData
                   String.)))))))

(deftest echo-multi-test
  (let [echo-workers (repeat 10 
                       (mdw/new-worker
                        "echo-multi"
                        "tcp://localhost:5555"
                        (fn [request reply]
                          ;;(Thread/sleep 500)
                          (doall (map #(.add reply %)
                                      (.toArray request))))))
        echo-client (mdc/new-client "tcp://localhost:5555")]

    (doseq [w echo-workers]
      (future (mdw/run w)))

    (time
    (doseq [x random-strings]
      (let [reply (mdc/send! echo-client "echo-multi" x)]
        (is (= x (-> (.toArray reply)
                           first
                           .getData
                           String.))))))))

