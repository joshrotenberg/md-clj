(ns md-clj.test.http
  (:use md-clj.broker
        md-clj.worker
        md-clj.client)
  (:use [clojure.contrib.duck-streams :only [slurp*]])
  (:use [compojure.core :only [defroutes GET POST]]
        [ring.adapter.jetty :only [run-jetty]])
  (:use clojure.test
        re-rand)
  (:require [clj-http.client :as http-client])
  (:import org.zeromq.ZMsg))

;; this test puts an http front end on a client/broker/worker


;; this  is the worker function. it just echos any frames from the request
;; into the reply
(defn echo-handler
  [request reply]
  (doall (map #(.add reply %)  (.toArray request))))

;; the echo worker itself
(def echo-http-worker
  (new-worker "echo-http" "tcp://localhost:5555" false echo-handler))

;; the client
(def echo-http-md-client (new-client "tcp://localhost:5555" false))

;; at this point we have a complete echo system for the backend.

;; this handler takes a request from the http front end, sends the post body
;; as-is via the backend client to the broker, and gets the response and returns
;; it to the http front end, which sends it to the client
(defn echo-http-handler [request]
  (let [body (slurp* (:body request))
        request (ZMsg.)
        _ (.addString request body)
        reply (send* echo-http-md-client "echo-http" request)]
    (-> (.toArray reply)
        first
        .getData
        String.)))

(defroutes routes
  (POST "/echo" request (echo-http-handler request)))

(def random-strings (repeatedly 1000 #(re-rand #"[A-Za-z0-9]{10}")))

(deftest http-test
  ;; start the broker
  (future (start-broker "tcp://*:5555" false))
  ;; start the worker
  (future (run echo-http-worker))
  ;; and start the http front end
  (future (run-jetty routes {:port 5556}))
  
  ;; first just test the backend directly
  (doseq [x random-strings]
    (let [request (ZMsg.)
          _ (.addString request x)
          reply (send* echo-http-md-client "echo-http" request)]
      (is (= x (-> (.toArray reply)
                   first
                   .getData
                   String.)))))
  
  ;; now test it through the http front end
  (doseq [x random-strings]
    (is (= x (:body (http-client/post "http://localhost:5556/echo" {:body x}))))
    ))