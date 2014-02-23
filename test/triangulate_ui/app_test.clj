(ns triangulate-ui.app-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [triangulate.model :refer [->Point]]
            [triangulate-ui.app :refer [add-adapter
                                        add-point
                                        change-point
                                        clear-points
                                        create-event
                                        delete-point
                                        process-events
                                        process-one-event
                                        start
                                        subscribe
                                        unsubscribe
                                        update-triangles]]
            [triangulate-ui.protocols :as protocols]))


(facts "about adapters"
       (fact "add-adapter"
             (let [world {:adapters {}}
                   new-world {:adapters {:adapter ..adapter..}}]
               (add-adapter world :adapter ..adapter..) => new-world))
       (fact "start"
             (let [world {:adapters {:A ..A.. :B ..B..}
                          :subscribers {}}
                   world-a {:adapters {:A ..A.. :B ..B..}
                              :subscribers {:foo #{..handler..}
                                            :bar #{..handler..}}}
                   world-b {:adapters {:A ..A.. :B ..B..}
                              :subscribers {:foo #{..handler..}
                                            :bar #{..handler..
                                                   ..other-handler..}}}]
               (start world) => world-b
               (provided (protocols/start! ..A.. world) => world-a
                         (protocols/start! ..B.. world-a) => world-b))))


(facts "about events"
       (fact "create-event"
             (let [world {:notifications (clojure.lang.PersistentQueue/EMPTY)}
                   new-world {:notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                                   '(..event-name.. ..x.. ..y..))}]
               (create-event world ..event-name.. ..x.. ..y..) => new-world))
       (fact "process-events"
             (let [world {:notifications ..notifications..
                          :subscribers {:foo #{..handler..}}}]
               (process-events world) => world)
             (let [world {:notifications (conj (clojure.lang.PersistentQueue/EMPTY) '(..e1..))
                          :subscribers {..e2.. #{..handler..}}}
                   new-world {:notifications (clojure.lang.PersistentQueue/EMPTY)
                              :subscribers {..e2.. #{..handler..}}}]
               (process-events world) => new-world)
             (let [world {:notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                             '(..e1..)
                                             '(..e2.. ..x.. ..y..))
                          :subscribers {..e2.. #{--handler--}
                                        ..e3.. #{--other-handler--}}}
                   new-world {:notifications (clojure.lang.PersistentQueue/EMPTY)
                              :subscribers {..e2.. #{--handler--}
                                            ..e3.. #{--other-handler--}}}]
               (process-events world) => new-world))
       (fact "process-one-event"
             ;; No event to process
             (let [world {:notifications (clojure.lang.PersistentQueue/EMPTY)}]
               (process-one-event world) => world)
             ;; Events to process but no handler
             (let [world {:notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                               '(..e1..)
                                               '(..e2..))
                          :subscribers {}}
                   new-world {:notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                                   '(..e2..))
                          :subscribers {}}]
               (process-one-event world) => new-world)
             ;; Multiple handlers
             (let [handlers [(fn [world event-name & args] (assoc world :handler-called args))
                             (fn [world event-name & args] (assoc world :other-handler-called args))]
                   world {:notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                               '(:event ..a.. ..b..))
                          :subscribers {:event handlers}}
                   new-world {:handler-called '(..a.. ..b..)
                            :notifications (clojure.lang.PersistentQueue/EMPTY)
                            :other-handler-called '(..a.. ..b..)
                            :subscribers {:event handlers}}]
               (process-one-event world) => new-world))
       (fact "subscribe"
             (let [world {:subscribers {..e1.. #{..h1..}}}
                   world-a {:subscribers {..e1.. #{..h1.. ..h2..}}}
                   world-b {:subscribers {..e1.. #{..h1..}
                                          ..e2.. #{..h2..}}}]
               (subscribe world ..e1.. ..h1..) => world
               (subscribe world ..e1.. ..h2..) => world-a
               (subscribe world ..e2.. ..h2..) => world-b))
       (fact "unsubscribe"
             (let [world {:subscribers {..event.. #{..handler..}}}
                   new-world {:subscribers {}}]
               (unsubscribe world ..event.. ..handler..) => new-world
               (unsubscribe world ..other-event.. ..handler..) => world
               (unsubscribe world ..event.. ..other-handler..) => world)))


(facts "about points"
       (fact "add-point"
             (let [world {:points [..A.. ..B..]
                          :notifications (clojure.lang.PersistentQueue/EMPTY)}
                   new-world {:points [..A.. ..B.. ..C..]
                              :notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                                   '(:points-changed))}]
               (add-point world ..x.. ..y..) => new-world
               (provided (->Point ..x.. ..y..) => ..C..)))
       (fact "change-point"
             (let [world {:points [..A.. ..B.. ..C..]
                          :notifications (clojure.lang.PersistentQueue/EMPTY)}
                   new-world {:points [..A.. ..D.. ..C..]
                              :notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                                   '(:points-changed))}]
               (change-point world 1 ..x.. ..y..) => new-world
               (provided (->Point ..x.. ..y..) => ..D..)))
       (fact "clear-points"
             (let [world {:points [..A.. ..B..]
                          :notifications (clojure.lang.PersistentQueue/EMPTY)}
                   new-world {:points []
                              :notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                                   '(:points-changed))}]
               (clear-points world) => new-world))
       (fact "delete-point"
             (let [world {:points [..A.. ..B.. ..C..]
                          :notifications (clojure.lang.PersistentQueue/EMPTY)}
                   new-world {:points [..A.. ..C..]
                              :notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                                   '(:points-changed))}]
               (delete-point world 1) => new-world)))


(facts "about triangles"
       (fact "update-triangles"
             (let [world {:triangles ..triangles..
                          :notifications (clojure.lang.PersistentQueue/EMPTY)}
                   new-world {:triangles ..new-triangles..
                              :notifications (conj (clojure.lang.PersistentQueue/EMPTY)
                                                   '(:triangles-changed))}]
               (update-triangles world ..new-triangles..) => new-world)))
