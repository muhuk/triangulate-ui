(ns triangulate-ui.triangulator
  (:require [triangulate.core :refer [triangulate]]
            [triangulate-ui.app :as app]
            [triangulate-ui.executors :refer [->DelayedExecutor]]
            [triangulate-ui.protocols :refer [IService]]))


(declare points-changed-handler)


(defrecord Triangulator [world executor]
  IService
  (start! [_ world]
          (app/subscribe world :points-changed points-changed-handler))
  (stop! [_ world]
         (app/unsubscribe world :points-changed points-changed-handler)))


(defn points-changed-handler [world-state event-name & args]
  (let [executor (get-in world-state [:adapters :triangulator :executor])
        points (:points world-state)
        world (get-in world-state [:adapters :triangulator :world])]
    (if (> (count points) 3)
      (do
        (.run executor
              #(swap! world
                      app/update-triangles
                      (triangulate points)))
        world-state)
      (assoc world-state :triangles []))))


(defn make-triangulator [world]
  (->Triangulator world (->DelayedExecutor (agent 0) 1000)))
