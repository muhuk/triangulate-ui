(ns triangulate-ui.executors
  (:require [triangulate-ui.protocols :refer [IExecutor]]))


(declare try-executing)


(defrecord DelayedExecutor [last-executed-agent min-interval]
  IExecutor
  (run [_ task] (send last-executed-agent
                      try-executing
                      min-interval
                      last-executed-agent
                      task)))


(defn execute-task [last-executed-time task]
  (let [now (java.lang.System/currentTimeMillis)]
       (task)
       now))


(defn try-executing [last-executed-time min-interval last-executed-agent task]
  (let [now (java.lang.System/currentTimeMillis)]
    (if (> now (+ last-executed-time min-interval))
        (do (send last-executed-agent execute-task task)
            now)
        last-executed-time)))
