(ns triangulate-ui.app
  (:require [triangulate.model :refer [->Point]]
            [triangulate-ui.protocols :as protocols]))


(declare create-event process-one-event)


(defn add-adapter [world adapter-name adapter]
  (update-in world [:adapters] conj [adapter-name adapter]))


(defn add-point [world x y]
  (let [point (->Point x y)]
    (-> world
        (update-in [:points] conj point)
        (create-event :points-changed))))


(defn change-point [world idx x y]
  (let [point (->Point x y)]
    (-> world
        (update-in [:points] assoc idx point)
        (create-event :points-changed))))


(defn create-event [world event-name & args]
  (update-in world [:notifications] conj (conj args event-name)))


(defn clear-points [world]
  (-> world
      (assoc :points [])
      (create-event :points-changed)))


(defn delete-point [world idx]
  (let [remove-elem (fn [points i] (if (> i 0)
                                     (vec (concat (subvec points 0 i)
                                                  (subvec points (inc i))))
                                     (vec (rest points))))]
    (-> world
        (update-in [:points] remove-elem idx)
        (create-event :points-changed))))


(defn make-world []
  {:adapters {}
   :notifications (clojure.lang.PersistentQueue/EMPTY)
   :points []
   :subscribers {}
   :triangles []})


(defn process-events [world]
  (if-not (empty? (:notifications world))
    (recur (process-one-event world))
    world))


(defn process-one-event [world]
  (let [[event-name & args] (peek (:notifications world))]
    (loop [world (update-in world [:notifications] pop)
           handlers (get-in world [:subscribers event-name])]
      (if (empty? handlers)
        world
        (recur (apply (first handlers)
                      (concat (list world event-name) args))
               (rest handlers))))))


(defn start [world]
  (loop [world world
         adapters (:adapters world)]
    (if (empty? adapters)
      world
      (recur (protocols/start! (second (first adapters)) world)
             (rest adapters)))))


(defn subscribe [world event-name handler]
  (update-in world [:subscribers event-name] #(set (conj % handler))))


(defn unsubscribe [world event-name handler]
  (-> world
      (update-in [:subscribers event-name] #(set (remove #{handler} %)))
      (update-in [:subscribers] #(into {} (remove (comp empty? second) %)))))


(defn update-triangles [world new-triangles]
  (-> world
      (assoc :triangles new-triangles)
      (create-event :triangles-changed)))
