(ns sketches.book01
  (:require
   [clojure.core.matrix :refer [add sub mul div]]
   [sketches.utils :as u]
   [sketches.global :as g]
   [reagent.core :as r]
   [promesa.core :as p :include-macros true]))


;; sketch01 --------------------------------------------------------------------


(defn Canvas [{:keys [width height draw]}]
  (let [dom-node (r/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (reset! dom-node (r/dom-node this)))

      :component-did-update
      (fn []
        (draw @dom-node))
      
      :reagent-render
      (fn []
        @g/window-size
        [:canvas (merge {:style {:width width :height height}}
                        (when-let [d @dom-node]
                          {:width (.-clientWidth d)
                           :height (.-clientHeight d)}))])})))

(defn best-distrib [n size aspect]
  (let [get-dist (comp Math/abs -)
        get-aspect #(apply / %)

        result
        (fn [x]
          (let [y (-> (/ n x) Math/ceil)
                result [x y]

                aspect' (get-aspect (div size result))
                aspect-dist (get-dist aspect aspect')

                n' (* x y)
                n-dist (/ (get-dist n n') n)]
            {:dist (+ n-dist aspect-dist)
             :result result}))]
    
    (->> (map result (range 1 (inc n)))
         (sort-by :dist)
         (map :result)
         first)))

(defonce state01 (r/atom {:imgs nil}))

(defn sketch01 []
  (let [dir "imgs/monopoly"
        max-size [1200 1200]
        n 25
        aspect 1

        max-tile-size (div max-size
                           (best-distrib n max-size aspect))
        
        load-img
        #(u/load-img "img" {:path %
                            :width (max-tile-size 0)
                            :height (max-tile-size 1)})
        
        load
        (fn [] 
          (-> (u/ajax {:uri "ls"
                       :method :post
                       :params {:dir dir}}) 
              (p/then #(p/all (map load-img %)))
              (p/then #(swap! state01 assoc :imgs (cycle %))) 
              (p/catch #(u/log (clj->js %)))))
        
        draw
        (fn [canvas]
          (let [ctx (u/ctx2d canvas)
                size (u/get-size canvas)
                tiles (best-distrib n size 1)
                _ (u/log2 tiles)
                {:keys [imgs]} @state01
                tile-size (div size tiles)]
            (when imgs
              (doseq [n-x (range (tiles 0))
                      n-y (range (tiles 1))
                      :let [n [n-x n-y]
                            pos (mul n tile-size)
                            img (->> n (add 1) (apply *) (nth imgs))]]
                (u/draw-image ctx img
                              [[0 0] (u/get-size img)]
                              [pos tile-size])))))]

    (load)

    (fn []
      (let [{:keys [imgs time]} @state01
            max (mul )]
        [:div {:style {:height "80vh"
                       :position :relative
                       :margin "10vh"
                       :max-width (max-size 0)
                       :max-height (max-size 1)
                       :background-color "white"}}
         (when imgs
           [Canvas {:width "100%" 
                    :height "100%"
                    :draw draw}])]))))


;; sketches --------------------------------------------------------------------

(def sketches
  [{:compo sketch01}])
