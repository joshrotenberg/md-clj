(ns md-clj.test.reverse
  (:use clojure.test
        re-rand)
  (:require [md-clj.broker :only [start-broker] :as mdb]
            [md-clj.worker :only [new-worker run] :as mdw]
            [md-clj.client :only [new-client send!] :as mdc])
  (:import org.zeromq.ZMsg))

;; start our broker in a future 
(future (mdb/start-broker "tcp://*:5555" false))

(defn reverse-fn
  [req rep]
  (doseq [f (.toArray req)]
    (.addString rep (apply str (reverse (String. (.getData f)))))))

(deftest reverse-test
  (let [reverse-worker (mdw/new-worker
                        "reverse" "tcp://localhost:5555" false reverse-fn)
        reverse-client (mdc/new-client "tcp://localhost:5555" false)]
    
    (future (mdw/run reverse-worker))
    (let [reply (mdc/send! reverse-client "reverse" ["bar" "foo"])]
      (is (= "rab" (-> (.toArray reply)
                       first
                       .getData
                       String.)))
      (is (= "oof" (-> (.toArray reply)
                       second
                       .getData
                       String.))))))


