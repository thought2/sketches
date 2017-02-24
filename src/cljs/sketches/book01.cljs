(ns sketches.book01
  (:require
   [clojure.core.matrix :refer [add sub mul div]]
   [sketches.utils :as u]
   [reagent.core :as r]
   [promesa.core :as p :include-macros true]))


;; sketch01 --------------------------------------------------------------------

(defn Canvas [{:keys [width height draw]}]
  (let [state (r/atom {:size [100 100]})
        update-size (fn [el]
                      (when el 
                        (let [size (u/get-real-size el)
                              ctx (u/ctx2d el)]
                          (swap! state assoc :size size)
                          (u/req-anim #(draw ctx size)))))]
    (fn []
      (let [{:keys [size]} @state]
        [:canvas {:style {:width width :height height} 
                  :ref update-size
                  :width (size 0)
                  :height (size 1)}]))))

(defonce state01 (r/atom {:imgs nil}))

(defn sketch01 []
  (let [dir "imgs/monopoly"
        
        load
        (fn [] 
          (-> (u/ajax {:uri "ls"
                       :method :post
                       :params {:dir dir}})
              (p/then (partial map #(u/load-img "/img" {:path %
                                                        :width 300})))
              (p/then #(p/all %))
              (p/then #(swap! state01 assoc :imgs %)) 
              (p/catch #(u/log (clj->js %)))))
        
        draw
        (fn [ctx size]
          (let [{:keys [imgs]} @state01
                size' [0.2 0.2]]
            (when imgs
              (let [img (first imgs)]
                (doseq [n-x (range 5)
                        n-y (range 5)
                        :let [n [n-x n-y]
                              pos (mul n size')
                              img (->> n (add 1) (apply *) (nth imgs))]]
                  (u/draw-image ctx img
                                [[0 0] (u/get-size img)]
                                (mul [pos size'] size)))))))]

    (load)

    (fn []
      (let [{:keys [imgs time]} @state01]
        [:div {:style {:height "80vh"
                       :position :relative
                       :margin "60"
                       :background-color "white"}}
         
         (when imgs [Canvas {:width "100%"
                             :height "100%"
                             :draw draw}])]))))


;; sketches --------------------------------------------------------------------

(def sketches
  [{:compo sketch01}])
