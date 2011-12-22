(ns md-clj.test.reverse
  (:use clojure.test
        md-clj.core
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
                        :reverse "tcp://localhost:5555" reverse-fn)
        reverse-client (mdc/new-client "tcp://localhost:5555")]
    
    (future (mdw/run reverse-worker))
    (let [reply (mdc/send! reverse-client :reverse ["bar" "foo"])]
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

    ;; XXX note: if the client sends a single element collection in the request
    ;; the worker has no idea that it is a collection, and treats it as it would
    ;; a single item request. thats just the way life is, so workers should
    ;; be defensive if there is a chance they may receive collections of varying
    ;; lengths, where varying means singular or plural
    (future (mdw/as-worker :reverse-more ep
                           (if (coll? request)
                             (map #(apply str (reverse (String. %))) request)
                             (apply str (reverse (String. request))))))
  
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
          reply-more-just-one (mdc/as-client :reverse-more ep '("just"))
          ;; and you can mix strings and byte arrays
          reply-more-vec-mix (mdc/as-client :reverse-more ep [(.getBytes "one")
                                                              "two"])
          ;; async clients work similarly with the following exceptions:
          ;; each element in the collection is sent independently, and all
          ;; elements are sent before any responses are fetched. responses
          ;; should be in the same order requests were sent. this is a handy
          ;; way to send a bunch of single item requests at once, but of course
          ;; you can also make a worker than handles multiple items at the same
          ;; time

          ;; makes two async requests, one for boof and one for chuh. once
          ;; both have been sent, calls recv and collects/returns the response
          reply-one-async (mdc/as-client-async :reverse-one ep ["boof" "chuh"])

          ;; makes three async requests, one for duh, one for [boof],
          ;; and one for [what, now]. this is a decent way to send a
          ;; batch of requests that may contain multiple items.
          ;; see the note above regarding workers that
          ;; may need to handle requests with one or more items.
          reply-more-async (mdc/as-client-async :reverse-more ep
                                                ["duh"
                                                 ["boof"]
                                                 ["what" "now"]])
          ;; you can really get your async on by firing off a bunch of async
          ;; requests in a future and come back later for the results.
          ft (future (mdc/as-client-async :reverse-one ep '("one" "two" "three"
                                                            "four" "five" "six"
                                                            "seven" "eight")))]
      
      ;; the return values are either a single byte array or a sequence
      ;; of byte arrays, depending on what the worker function returns

      (is (= "helb" (String. reply-one)))
      (is (= (String. reply-one-bytes) (String. reply-one)))
      (is (= "eno" (String. (first reply-more))))
      (is (= "owt" (String. (second reply-more))))
      (is (= (map #(String. %) reply-more)
             (map #(String. %) reply-more-vec)))
      (is (= "tsuj" (String. reply-more-just-one)))
      (is (= (map #(String. %) reply-more-vec)
             (map #(String. %) reply-more-vec-mix)))
      (is (= '("foob" "huhc")  (map #(String. %) reply-one-async)))
      (is (= "hud" (String. (first reply-more-async))))
      (is (= "foob" (String. (second reply-more-async))))
      (is (= '("tahw" "won") (map #(String. %) (last reply-more-async))))
      (is true (future-done? ft))
      (is (= '("eno" "owt" "eerht" "ruof" "evif" "xis" "neves" "thgie")
             (map #(String. %) @ft))))))
          

      

