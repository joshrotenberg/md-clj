(ns md-clj.test.rpc
  (:use clojure.test
        md-clj.core
        re-rand
        cheshire.core)
  (:require [md-clj.broker :only [start-broker] :as mdb]
            [md-clj.worker :only [new-worker run as-worker] :as mdw]
            [md-clj.client :only [new-client send!] :as mdc])
  (:import org.zeromq.ZMsg))

;; send some clojure to a worker and have the worker run it.
;; you probably wouldn't do this in real life. 
(deftest clj-rpc
  (let [ep "tcp://localhost:5555"
        broker (future (mdb/start-broker "tcp://*:5555" false))]

    (future (mdw/as-worker :clj-rpc ep
                           (eval (read-string (String. request)))))

    (let [f (with-message-type :as-number
              (mdc/as-client :clj-rpc ep (str '((comp str +) 8 8 8))))]
      (is (= 24 f)))))

;; silly json-rpc example. you might do something like this in a situation
;; where you want a single worker to handle multiple types of requests.
(deftest json-rpc
  (let [ep "tcp://localhost:5555"
        broker (future (mdb/start-broker "tcp://*:5555" false))
        add (fn [a b] (+ a b))
        multiply (fn [a b] (* a b))]
    
    (future (with-message-type :as-string
              (mdw/as-worker :json-rpc ep
                             (let [r (parse-string request true)
                                   res (condp = (:fn r)
                                         "add" (add (:a r) (:b r))
                                         "multiply" (multiply (:a r) (:b r)))]
                               (generate-string {:res res})))))

    (let [a-response (with-message-type :as-string
                       (mdc/as-client :json-rpc ep
                                      (generate-string {:fn "add"
                                                        :a 2
                                                        :b 4})))
          m-response (with-message-type :as-string
                       (mdc/as-client :json-rpc ep
                                      (generate-string {:fn "multiply"
                                                        :a 2
                                                        :b 4})))]
      (is (= 6 (:res (parse-string a-response true))))
      (is (= 8 (:res (parse-string m-response true)))))))


