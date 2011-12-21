(ns md-clj.test.reverse
  (:use clojure.test
        re-rand)
  (:require [md-clj.broker :only [start-broker] :as mdb]
            [md-clj.worker :only [new-worker run as-worker] :as mdw]
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

;; another way to create a client and worker in a more DSL'y style.

(deftest as-test
  (let [ep "tcp://localhost:5555"]

    ;; as-worker takes a service name keyword, an endpoint, and then
    ;; the body is the actual service function
    
    ;; this worker expects a scalar request an returns a scalar reply
    (future (mdw/as-worker :reverse-one ep
                           (apply str (reverse (String. request)))))
    
    ;; this worker expects a sequence request and returns a sequence reply
    (future (mdw/as-worker :reverse-more ep
                           (map #(apply str (reverse (String. %))) request)))
  
    ;; as-client takes a service name keyword, an endpoint, and the body should
    ;; return a request. the call itself will return the worker's response
    
    (let [;; a single value string request
          reply-one (mdc/as-client :reverse-one ep "bleh")
          ;; or as an array of bytes
          reply-one-bytes (mdc/as-client :reverse-one ep (.getBytes "bleh"))
          ;; a list
          reply-more (mdc/as-client :reverse-more ep '("one" "two"))
          ;; or a vector
          reply-more-vec (mdc/as-client :reverse-more ep ["one" "two"])
          ;; and you can mix strings and byte arrays
          reply-more-vec-mix (mdc/as-client :reverse-more ep [(.getBytes "one")
                                                              "two"])
          ]

      ;; the return values are either a single byte array or a sequence
      ;; of byte arrays, depending on what the worker function returns

      (is (= "helb" (String. reply-one)))
      (is (= (String. reply-one-bytes) (String. reply-one)))
      (is (= "eno" (String. (first reply-more))))
      (is (= "owt" (String. (second reply-more))))
      (is (= (map #(String. %) reply-more)
             (map #(String. %) reply-more-vec)))
      (is (= (map #(String. %) reply-more-vec)
             (map #(String. %) reply-more-vec-mix))))))

      

