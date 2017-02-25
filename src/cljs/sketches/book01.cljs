(ns sketches.book01
  (:require 
   [clojure.core.matrix :refer [add sub mul div]]
   [sketches.utils :as u]
   [sketches.global :as g]
   [reagent.core :as r]
   [promesa.core :as p :include-macros true]))


;; sketch01 --------------------------------------------------------------------


(defn Canvas [{:keys [width height draw on-resize anim?]}]
  (let [dom-node (r/atom nil)
        running (r/atom false)
        size (r/atom nil)
        get-size #(some-> @dom-node u/get-real-size)]
    
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [cnv (r/dom-node this)]
          (reset! dom-node cnv)
          (reset! running true)
          (when anim?
            (u/req-anim (fn anim [t] 
                          (draw cnv t)
                          (when @running
                            (u/req-anim anim)))))))

      :component-did-update
      (fn []
        (let [d @dom-node
              size' (u/get-real-size d)]
          (when (not= size' @size)
            (on-resize d))
          (reset! size size')))

      :component-will-unmount
      (fn []
        (reset! running false))
      
      :reagent-render
      (fn []
        (u/log "REN")
        @g/window-size
        [:canvas (merge {:style {:width width
                                 :height height}}
                        (when-let [size (get-size)] 
                          {:width (size 0)
                           :height (size 1)}))])})))


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

(defonce state01 (r/atom {:imgs nil
                          :tiles nil
                          :cnv-size nil}))



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
              (p/then #(swap! state01 assoc :imgs %)) 
              (p/catch #(u/log (clj->js %)))))        

        mk-tiles
        (fn [tiles-cnt size imgs] 
          (let [steps (u/cart (mapv range tiles-cnt))
                tile-size (div size tiles-cnt)]
            (map (fn [step img]
                   (let [pos (mul step tile-size)]
                     {:frame [pos tile-size]
                      :img img}))
                 steps imgs)))

        init-tiles
        (fn []
          (let [{:keys [cnv-size imgs]} @state01] 
            (swap! state01 assoc :tiles
                   (-> (best-distrib n cnv-size aspect)
                       (mk-tiles cnv-size imgs)))))
        
        draw
        (fn [canvas t]          
          (let [{:keys [tiles cnv-size imgs]} @state01
                ctx (u/ctx2d canvas)]
            (u/log2 cnv-size)
            (when tiles
              (doseq [{:keys [frame img]} tiles]
                (u/draw-image ctx img
                              [[0 0] (u/get-size img)]
                              frame)))))
        
        resize
        (fn [cnv]
          (swap! state01 assoc :cnv-size (u/get-size cnv)))
        
        init
        (fn []
          (load)
          (add-watch state01 :watch
                     (fn [_ _ old-st new-st]
                       (let [ks [:imgs :cnv-size]
                             changed? #(not= (old-st %) (new-st %))]
                         (when (and (some changed? ks)
                                    (every? new-st ks))
                           (init-tiles))))))]


    (init)

    (fn []
      (let [{:keys [imgs time]} @state01
            max (mul )]
        [:div {:style {:height "80vh"
                       :position :relative
                       :margin "10vh"
                       :max-width (max-size 0)
                       :max-height (max-size 1)
                       :background-color "white"}}
         (u/log (clj->js @state01))
         [Canvas {:width "100%" 
                  :height "100%"
                  ;;:draw draw
                  :on-resize (fn [c] (resize c) (draw c 0))
                  :anim? false
                  }]]))))


;; sketches --------------------------------------------------------------------

(def sketches
  [{:compo sketch01}])
