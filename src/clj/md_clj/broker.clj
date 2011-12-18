(ns md-clj.broker
  (:import mdbroker))

(defn start-broker
  "Start a broker on the specified endpoint."
  [endpoint verbose]
  (let [broker (mdbroker. verbose)]
    (doto broker
      (.bind endpoint)
      (.mediate))))