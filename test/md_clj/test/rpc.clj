(ns md-clj.test.rpc
  (:use clojure.test
        md-clj.core
        re-rand)
  (:require [md-clj.broker :only [start-broker] :as mdb]
            [md-clj.worker :only [new-worker run as-worker] :as mdw]
            [md-clj.client :only [new-client send!] :as mdc])
  (:import org.zeromq.ZMsg))


(deftest clj-rpc
  (let [ep "tcp://localhost:5555"
        broker (future (mdb/start-broker "tcp://*:5555" false))]

    (future (mdw/as-worker :clj-rpc ep
                           (eval (read-string (String. request)))))

    (let [f (mdc/as-client :clj-rpc ep (str '((comp str +) 8 8 8)))]

      (is (= "24" (String. f))))))

