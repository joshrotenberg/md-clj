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

See http://rfc.zeromq.org/spec:7

Note that this has nothing to do with the mailing list majordomo:

> The Majordomo Protocol (MDP) defines a reliable service-oriented
> request-reply dialog between a set of client applications, a broker
> and a set of worker applications. MDP covers presence, heartbeating,
> and service-oriented request-reply processing. It originated from the
> Majordomo pattern defined in Chapter 4 of the Guide.

This is a thin Clojure wrapper around the Java implementation.

## License

Copyright (C) 2011 Josh Rotenberg

Distributed under the Eclipse Public License, the same as Clojure.
