(ns triangulate-ui.protocols)


(defprotocol IExecutor
  (run [this task]))


(defprotocol IService
  (start! [this world])
  (stop! [this world]))
