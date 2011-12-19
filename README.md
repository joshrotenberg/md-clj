# md-clj

0MQ Majordomo for Clojure.

## tl;dr

This is a wrapper for [this](http://rfc.zeromq.org/spec:7) around the
relevant classes
[here](https://github.com/imatix/zguide/tree/master/examples/Java) by
Arkadiusz Orzechowski. Arkadiusz's code is included here directly with
only one small modification: the bind method in mdbroker.java has been
declared public instead of private as it exists in the imatix/zguide
repo.

## 0MQ Majordomo

See http://rfc.zeromq.org/spec:7 and http://zguide.zeromq.org/page:all#Service-Oriented-Reliable-Queuing-Majordomo-Pattern

Note that this has nothing to do with the mailing list majordomo:

> The Majordomo Protocol (MDP) defines a reliable service-oriented
> request-reply dialog between a set of client applications, a broker
> and a set of worker applications. MDP covers presence, heartbeating,
> and service-oriented request-reply processing. It originated from the
> Majordomo pattern defined in Chapter 4 of the Guide.

This is a thin Clojure wrapper around the Java implementation. I
started a pure Clojure implementation, but this works right now.

## Usage

The Majordomo Pattern provides a framework for building "Reliable
Service-Oriented Queues". These queues consist of three main
components: a worker, a broker and a client. The broker itself is
fairly generic and delegates work from clients to workers and
back. Workers implement a named function and register their
availability to do some work with the broker. Clients connect to the
broker and give it the name of the work to do and some data upon which
to 'work' on, and wait for a response.

For now this wrapper isn't very sophisticated, but it is fairly easy
to use all three components. It's also easy to use them all in the
same file for testing, and split them into separate processes later
once things are working:

```clojure
(ns my.app
  (:use md-clj.broker
        md-clj.worker
        md-clj.client)
(:import org.zeromq.ZMsg)

;; start our broker in a future 
(future (start-broker "tcp://*:5555" *verbose*))

;; create a simple echo worker
(def echo-worker (new-worker
                     "echo" "tcp://localhost:5555" *verbose*
                     (fn [request reply]
                       (doall (map #(.add reply %) (.toArray request))))))

;; run the worker
(future (run echo-worker))

;; create a client
(def echo-client (new-client "tcp://localhost:5555" *verbose*))

(let [request (ZMsg.)
      _ (.addString request "some string")
      reply (send* echo-client "echo" request)]
   (is (= "some string" (-> (.toArray reply)
                       first
                       .getData
                        String.)))

)
```

## License

Copyright (C) 2011 Josh Rotenberg

Distributed under the Eclipse Public License, the same as Clojure.
