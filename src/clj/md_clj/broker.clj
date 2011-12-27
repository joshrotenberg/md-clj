(ns md-clj.broker
  "Majordomo Broker"
  (:import mdbroker))

;; The broker sits between the client(s) and the worker(s), delegating tasks
;; from the client to an available worker and returning the worker's response
;; back to the client. Aside from the specified service requested by the client,
;; the broker has little interest in the payload itself. Current, the only
;; task necessary from the user's perspective is to start the broker on
;; the specified endpoint:

(defn start-broker
  "Start a broker on the specified endpoint, i.e.:
   (start-broker \"tcp://*:5555\" false)"
  [endpoint verbose]
  (let [broker (mdbroker. verbose)]
    (doto broker
      (.bind endpoint)
      (.mediate))))