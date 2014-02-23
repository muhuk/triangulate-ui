(ns triangulate-ui.gui
  (:require [triangulate.model :refer [->Point ->Triangle]]
            [triangulate-ui.app :as app]
            [triangulate-ui.protocols :refer [IService]])
  (:import (java.awt BorderLayout Color Dimension)
           (java.awt.event ActionListener ComponentListener)
           (java.awt.image BufferedImage)
           (javax.swing Box
                        BoxLayout
                        JButton
                        JFrame
                        JLabel
                        JOptionPane
                        JScrollPane
                        JTable
                        JTextField
                        ListSelectionModel)
           (javax.swing.table DefaultTableModel)))


(declare add-point
         add-add-point-listener
         add-clear-points-listener
         add-delete-point-listener
         add-resize-listener
         alert
         draw
         draw!
         draw-triangles!
         draw-points!
         make-scaler
         make-side-panel
         update-points-table)


(def EDGE-COLOR (Color. 0 127 127))
(def INITIAL-WIDTH 500)
(def INITIAL-HEIGHT 600)
(def MARGIN 30)
(def NEAR-ZERO (/ 1 1000000))
(def POINT-SIZE 5)
(def POINT-COLOR (Color. 255 0 255))
(def TITLE "triangulate-ui")


(defrecord GUI [components frame world]
  IService
  (start! [_ world-state]
          (add-add-point-listener world components)
          (add-clear-points-listener world components)
          (add-delete-point-listener world components)
          (add-resize-listener world components)
          (-> world-state
              (app/subscribe :points-changed draw)
              (app/subscribe :points-changed update-points-table)
              (app/subscribe :triangles-changed draw)
              (app/subscribe :window-resized draw)))
  (stop! [_ world-state]
         (-> world-state
              (app/unsubscribe :points-changed draw)
              (app/unsubscribe :points-changed update-points-table)
              (app/unsubscribe :triangles-changed draw)
              (app/unsubscribe :window-resized draw))))


(defn add-point [world components]
  (let [x (.getText (:add-point-x components))
        y (.getText (:add-point-y components))]
    (try
      (let [x (Double/parseDouble x)
            y (Double/parseDouble y)]
        (swap! world app/add-point x y)
        (.setText (:add-point-x components) "")
        (.setText (:add-point-y components) ""))
      (catch NumberFormatException e (alert "Cannot parse coordinates.")))))


(defn add-add-point-listener [world components]
  (let [actionListener (proxy
                         [ActionListener]
                         []
                         (actionPerformed [_] (add-point world components)))]
    (.addActionListener (:add-point-button components) actionListener)))


(defn add-clear-points-listener [world components]
  (let [actionListener (proxy
                         [ActionListener]
                         []
                         (actionPerformed [_] (swap! world app/clear-points)))]
    (.addActionListener (:clear-points-button components) actionListener)))


(defn add-delete-point-listener [world {:keys [delete-point-button
                                               points-table]}]
  (let [actionListener (proxy
                         [ActionListener]
                         []
                         (actionPerformed
                          [_]
                          (let [selectionIndex (.getSelectedRow points-table)]
                            (if (>= selectionIndex 0)
                              (do
                                (prn selectionIndex)
                                (.clearSelection points-table)
                                (swap! world app/delete-point selectionIndex))
                              (alert "No point is selected.")))))]
    (.addActionListener delete-point-button actionListener)))


(defn add-resize-listener [world {:keys [canvas image]}]
  (let [componentListener (proxy
                            [ComponentListener]
                            []
                            (componentResized [e] (let [component (.getComponent e)
                                                        width (.getWidth component)
                                                        height (.getHeight component)]
                                                    (reset! image (BufferedImage. width height BufferedImage/TYPE_INT_RGB))
                                                    (swap! world app/create-event :window-resized))))]
    (.addComponentListener canvas componentListener)))


(defn alert [message]
  (JOptionPane/showMessageDialog nil message "Error" JOptionPane/ERROR_MESSAGE))


(defn draw [world event-name & args]
  (let [canvas (get-in world [:adapters :gui :components :canvas])
        image-ref (get-in world [:adapters :gui :components :image])
        points (:points world)
        triangles (:triangles world)]
    (draw! canvas @image-ref points triangles))
  world)


(defn draw! [canvas image points triangles]
  (let [graphics (.createGraphics image)
        width (.getWidth image)
        height (.getHeight image)
        point-scaler (make-scaler points width height MARGIN)
        triangle-scaler (fn [t] (->Triangle (point-scaler (:a t))
                                            (point-scaler (:b t))
                                            (point-scaler (:c t))))
        scaled-points (vec (map point-scaler points))
        scaled-triangles (vec (map triangle-scaler triangles))]
    (.clearRect graphics 0 0 width height)
    (draw-triangles! graphics scaled-triangles)
    (draw-points! graphics scaled-points)
    (.repaint canvas)))


(defn draw-triangles! [graphics triangles]
  (.setColor graphics EDGE-COLOR)
  (doseq [triangle triangles]
    (let [{{x1 :x y1 :y} :a
           {x2 :x y2 :y} :b
           {x3 :x y3 :y} :c} triangle]
      (doto graphics
        (.drawLine x1 y1 x2 y2)
        (.drawLine x2 y2 x3 y3)
        (.drawLine x1 y1 x3 y3)))))


(defn draw-points! [graphics points]
  (let [offset (/ POINT-SIZE 2)]
    (.setColor graphics POINT-COLOR)
    (doseq [point points]
      (let [x (:x point)
            y (:y point)]
        (.fillOval graphics (- x offset) (- y offset) POINT-SIZE POINT-SIZE)))))


(defn make-canvas [image]
  (let [canvas (proxy [JLabel] [] (paint [graphics] (.drawImage graphics @image 0 0 this)))]
    (.setPreferredSize canvas (Dimension. INITIAL-WIDTH INITIAL-HEIGHT))
    canvas))


(defn make-frame [title canvas size-panel]
  (doto (JFrame. title)
    (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.add canvas)
    (.add size-panel BorderLayout/EAST)
    (.pack)
    (.show)))


(defn make-gui [world]
  (let [add-point-button (JButton. "Add Point")
        add-point-x (JTextField. 12)
        add-point-y (JTextField. 12)
        clear-points-button (JButton. "Clear Points")
        delete-point-button (JButton. "Delete Point")
        image (atom (BufferedImage. INITIAL-WIDTH INITIAL-HEIGHT BufferedImage/TYPE_INT_RGB))
        points-table (doto
                       (JTable. (proxy
                                  [DefaultTableModel]
                                  [(to-array ["Index" "x" "y"]) 0]
                                  (isCellEditable [_ _] false)))
                       (.setSelectionMode ListSelectionModel/SINGLE_SELECTION))
        canvas (make-canvas image)
        side-panel (make-side-panel add-point-button
                                    add-point-x
                                    add-point-y
                                    clear-points-button
                                    delete-point-button
                                    points-table)
        components {:add-point-button add-point-button
                    :add-point-x add-point-x
                    :add-point-y add-point-y
                    :canvas canvas
                    :clear-points-button clear-points-button
                    :delete-point-button delete-point-button
                    :image image
                    :points-table points-table}
        frame (make-frame TITLE canvas side-panel)]
    (->GUI components frame world)))


(defn make-side-panel [add-point-button
                       add-point-x
                       add-point-y
                       clear-points-button
                       delete-point-button
                       points-table]
  (let [add-form (doto
                   (Box. BoxLayout/X_AXIS)
                   (.add add-point-x)
                   (.add add-point-y)
                   (.add add-point-button))
        points-pane (doto
                      (Box. BoxLayout/Y_AXIS)
                      (.add (JScrollPane. points-table))
                      (.add (doto
                              (Box. BoxLayout/X_AXIS)
                              (.add delete-point-button)
                              (.add clear-points-button))))]
    (doto
      (Box.  BoxLayout/Y_AXIS)
      (.add add-form)
      (.add points-pane))))


(defn make-scaler [points width height margin]
  (if (empty? points)
    identity
    (let [xs (map :x points)
          ys (map :y points)
          original-left (apply min xs)
          original-top (apply min ys)
          original-width (max (- (apply max xs) original-left) NEAR-ZERO)
          original-height (max (- (apply max ys) original-top) NEAR-ZERO)
          scale-ratio-x (/ (- width (* 2 margin)) original-width)
          scale-ratio-y (/ (- height (* 2 margin)) original-height)]
      (fn [{:keys [x y]}] (->Point (+ (* (- x original-left) scale-ratio-x) margin)
                                   (+ (* (- y original-top) scale-ratio-y) margin))))))


(defn update-points-table [world event-name & args]
  (let [points (:points world)
        points-table (get-in world [:adapters :gui :components :points-table])
        table-model (cast DefaultTableModel (.getModel points-table))]
    (when (.getRowCount table-model)
      (doseq [_ (range (.getRowCount table-model))]
        (.removeRow table-model 0)))
    (doseq [[idx {:keys [x y]}] (map-indexed vector points)]
      (.addRow table-model (to-array [(str idx) (str x) (str y)])))
    world))
