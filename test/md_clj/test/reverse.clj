(ns md-clj.test.reverse
  (:use md-clj.broker
        md-clj.worker
        md-clj.client)
  (:use clojure.test
        re-rand)
  (:import org.zeromq.ZMsg))

;; start our broker in a future 
(future (start-broker "tcp://*:5555" false))

(defn reverse-fn
  [req rep]
  (doseq [f (.toArray req)]
    (.addString rep (apply str (reverse (String. (.getData f)))))))

(deftest reverse-test
  (let [reverse-worker (new-worker
                        "reverse" "tcp://localhost:5555" false reverse-fn)
        reverse-client (new-client "tcp://localhost:5555" false)]
    
    (future (run reverse-worker))
    (let [reply (send-strings reverse-client "reverse" ["bar" "foo"])]
      (is (= "rab" (-> (.toArray reply)
                       first
                       .getData
                       String.)))
      (is (= "oof" (-> (.toArray reply)
                       second
                       .getData
                       String.))))))


