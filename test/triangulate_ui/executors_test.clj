(ns triangulate-ui.executors-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [triangulate-ui.executors :refer [->DelayedExecutor
                                              execute-task
                                              try-executing]]))


(facts "about delayed executor"
       (fact "DelayedExecutor executes tasks only as frequently as min-interval."
             (let [logs (atom [])
                   task (fn [] (swap! logs
                                      conj
                                      (list (java.lang.System/currentTimeMillis)
                                            (.. java.lang.Thread currentThread getId))))
                   min-interval 50
                   sleep-duration 30
                   tasks-sent 20
                   tasks-executed 10
                   test-thread (.. java.lang.Thread currentThread getId)
                   executor (->DelayedExecutor (agent 0) min-interval)]
                  (dotimes [_ tasks-sent] (.run executor task)
                                          (java.lang.Thread/sleep sleep-duration)) => anything
                  (count @logs) => tasks-executed
                  (not-any? #(= % test-thread) (map second @logs)) => true
                  (let [timestamps (sort (map first @logs))
                        differences (map #(- %2 %1) timestamps (drop 1 timestamps))]
                       (every? #(> % min-interval) differences)) => true))
       (fact "execute-task runs task and updates last-executed-time."
             (let [now (java.lang.System/currentTimeMillis)]
                  (execute-task ..last-executed-time.. --task--) => (roughly now)
                  (provided (--task--) => anything :times 1)))
       (fact "try-executing doesn't execute until next execution time."
             (let [now (java.lang.System/currentTimeMillis)
                   last-executed-time (- now 1000)
                   min-interval 10000]
                  (try-executing last-executed-time min-interval ..agent.. ..task..) => last-executed-time
                  (provided (send ..agent.. ..task..) => anything :times 0)))
       (fact "try-executing executes when next execution time arrives."
             (let [now (java.lang.System/currentTimeMillis)
                   last-executed-time (- now 10000)
                   min-interval 1000]
                  (try-executing last-executed-time min-interval ..agent.. ..task..) => (roughly now)
                  (provided (send ..agent.. execute-task ..task..) => anything :times 1))))
