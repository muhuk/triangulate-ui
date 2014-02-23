(ns triangulate-ui.core
  (:require [triangulate-ui.app :as app]
            [triangulate-ui.gui :as gui]
            [triangulate-ui.triangulator :as triangulator])
  (:gen-class))


(defn make-app []
  (let [world (atom (app/make-world))]
    (swap! world #(-> %
                      (app/add-adapter :gui (gui/make-gui world))
                      (app/add-adapter :triangulator (triangulator/make-triangulator world))
                      (app/start)))
    world))


(defn -main
  "Entry point."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (let [world (make-app)]
    (swap! world #(-> %
                      (app/add-point 0 0)
                      (app/add-point 0 1)
                      (app/add-point 1 0)
                      (app/add-point 1 1)))
    (while true (do (java.lang.Thread/sleep 15)
                    (swap! world app/process-events)))))

