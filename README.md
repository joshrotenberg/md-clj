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

See:
* http://rfc.zeromq.org/spec:7 
* http://zguide.zeromq.org/page:all#Service-Oriented-Reliable-Queuing-Majordomo-Pattern

for the "possibly too long; (but) did read" explanation of the Majordomo system.

Note that this has nothing to do with the mailing list called Majordomo:

> The Majordomo Protocol (MDP) defines a reliable service-oriented
> request-reply dialog between a set of client applications, a broker
> and a set of worker applications. MDP covers presence, heartbeating,
> and service-oriented request-reply processing. It originated from the
> Majordomo pattern defined in Chapter 4 of the Guide.

"the Guide" here refers to the insanely complete and useful guide to 0MQ: http://zguide.zeromq.org/

This is a Clojure wrapper around the Java implementation. I started a
pure Clojure implementation, but this works right now.

## Usage

The Majordomo Pattern provides a framework for building "Reliable
Service-Oriented Queues". These queues consist of three main
components: a worker, a broker and a client. The broker itself is
fairly generic and delegates work from clients to workers and
back. Workers implement a named function and register their
availability to do some work with the broker. Clients connect to the
broker and give it the name of the work to do and some data upon which
to 'work' on, and wait for a response.

This generic pattern is pretty darn useful in a lot of situations. It
scales well and works in sitations where you want either/both
request/reply or queue'ed jobs that don't require a reply. Couple with
some kind of persistent storage like MongoDB and a more complex
payload type like JSON or Protocol Buffers, you can build stuff that's
pretty cool.

For now this wrapper isn't very sophisticated, but it is fairly easy
to use all three components. It's also easy to use them all in the
same file for testing, and split them into separate processes later
once things are working. 

There are two ways to use both the client and worker side: a more raw
API that gives you pretty close access to the underlying Java
implementation and a slightly more cooked API that looks more like a
fancy DSL. You can mix and match, using one for the client and another
for the worker without issue. Note also that 0MQ's cross language
capability is a high point, so you should be able to easily write,
say, your workers in Clojure and your clients in PHP/Java/Lua/C/C++
(that's the list of language I see with an example Majordomo API
already written in they guide, but any language that has 0MQ bindings
would work with a little effort).

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

;; and run the worker
(future (run echo-worker))

;; create a client
(def echo-client (new-client "tcp://localhost:5555" *verbose*))

;; send a request, and view the response
(let [request (ZMsg.)
      _ (.addString request "some string")
      reply (send! echo-client "echo" request)]
   (is (= "some string" (-> (.toArray reply)
                       first
                       .toString))))

```

## Examples

See the tests. Most are written as useful examples:
* echo - the standard echo scenario served up a few different ways
* reverse - standard and DSL APIs to create some reverse worker examples
* http - an http frontended parallel echo system

## TODO
* Request/reply handling should be wrapped up a little instead of using ZMsg directly.

## Status

* Changing a lot.

## License

Copyright (C) 2011 Josh Rotenberg
Portions (C)  2011 Arkadiusz Orzechowski

Distributed under the Eclipse Public License, the same as Clojure.
