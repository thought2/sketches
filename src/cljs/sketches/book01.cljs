(ns sketches.book01
  (:require 
   [clojure.core.matrix :refer [add sub mul div]]
   [sketches.utils :as u]
   [sketches.global :as g]
   [reagent.core :as r]
   [promesa.core :as p :include-macros true]))


;; sketch01 --------------------------------------------------------------------


(defn Canvas [{:keys [width height anim on-resize anim?]}]
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
            (u/req-anim (fn anim-wrap [t] 
                          (anim cnv t)
                          (when @running
                            (u/req-anim anim-wrap)))))))

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
  (let [get-aspect #(apply / %)

        result
        (fn [x]
          (let [y (-> (/ n x) Math/ceil)
                result [x y]
                aspect' (get-aspect (div size result))
                aspect-dist (u/dist aspect aspect')
                n' (* x y)
                n-dist (/ (u/dist n n') n)]
            {:dist (+ n-dist aspect-dist)
             :result result}))]
    
    (->> (map result (range 1 (inc n)))
         (sort-by :dist)
         (map :result)
         first)))


(defn mk-empty-img [size]
  (let [cnv (u/mk-elem "canvas")
        ctx (u/ctx2d cnv)]
    (u/clear-rect ctx [[0 0] size])
    (u/canvas->img cnv)))


(defonce state01 (r/atom {:imgs nil
                          :tiles nil
                          :cnv-size nil}))


(defn sketch01 []
  (let [dir "imgs/monopoly"
        max-size [1200 1200]
        n 25
        aspect 1
        n-empties 8
        waiting-rng [0 20000]
        fading-rng [1000 4000]

        max-tile-size (div max-size
                           (best-distrib n max-size aspect))
        
        load-img
        #(u/load-img "img" {:path %
                            :width (max-tile-size 0)
                            :height (max-tile-size 1)})
        
        waiting->fading
        (fn [{:keys [frame img]} t]
          {:state :fading
           :frame frame
           :img1 img
           :img2 (rand-nth (@state01 :imgs))
           :start t
           :end (+ t (u/rand-between fading-rng))})

        fading->waiting
        (fn [{:keys [frame img2]} t]
          {:state :waiting
           :frame frame
           :img img2
           :end (+ t (u/rand-between waiting-rng))})
        
        mk-tiles
        (fn [tiles-cnt size imgs] 
          (let [steps (u/cart (mapv range tiles-cnt))
                tile-size (div size tiles-cnt)]
            (map (fn [step img]
                   (let [pos (mul step tile-size)]
                     (fading->waiting {:frame [pos tile-size]
                                       :img2 img}
                                      0)))
                 steps imgs)))        

        draw-waiting
        (fn [ctx tile]
          (let [{:keys [img frame]} tile]
            (u/draw-image ctx img
                          [[0 0] (u/get-size img)]
                          frame)))

        draw-fading
        (fn [ctx tile t]
          (let [{:keys [img1 img2 frame start end]} tile 
                perc (u/clamp [start end] t)
                draw-alpha (fn [img perc]
                             (u/global-alpha ctx perc)
                             (u/draw-image ctx img
                                           [[0 0] (u/get-size img)]
                                           frame))]
            (draw-alpha img1 (- 1 perc))
            (draw-alpha img2 perc)
            (u/global-alpha ctx 1)))
        
        draw
        (fn [new-st cnv t]
          (let [{:keys [tiles cnv-size imgs]} new-st
                ctx (u/ctx2d cnv)
                size (u/get-size cnv)]
            (u/clear-rect ctx [[0 0] size]) 
            (when tiles
              (doseq [{:keys [state] :as tile} tiles]
                (condp = state
                  :waiting (draw-waiting ctx tile)
                  :fading (draw-fading ctx tile t))))))
        
        update-tiles
        (fn [t tiles]
          (for [{:keys [state imgs start end] :as tile} tiles]
            (condp = state
              :waiting (if (< end t)
                         (waiting->fading tile t)
                         tile)
              :fading (if (< end t)
                        (fading->waiting tile t)
                        tile))))
        
        anim!
        (fn [canvas t]
          (let [new-st (swap! state01 update :tiles #(update-tiles t %))]
            (draw new-st canvas t)))

        load!
        (fn [] 
          (-> (u/ajax {:uri "ls"
                       :method :post
                       :params {:dir dir}}) 
              (p/then #(p/all (map load-img %))) 
              (p/then (fn [imgs]
                        (let [imgs' (-> (repeat n-empties (mk-empty-img [1 1]))
                                        (concat imgs)
                                        shuffle)]
                          (swap! state01 assoc :imgs imgs'))))))        

        init-tiles!
        (fn []
          (let [{:keys [cnv-size imgs]} @state01] 
            (swap! state01 assoc :tiles
                   (-> (best-distrib n cnv-size aspect)
                       (mk-tiles cnv-size imgs)))))
        
        resize!
        (fn [cnv]
          (swap! state01 assoc :cnv-size (u/get-size cnv)))
        
        init!
        (fn []
          (load!)
          (add-watch state01 :watch
                     (fn [_ _ old-st new-st]
                       (let [ks [:imgs :cnv-size]
                             changed? #(not= (old-st %) (new-st %))]
                         (when (and (some changed? ks)
                                    (every? new-st ks))
                           (init-tiles!))))))]

    (init!)

    (fn []
      (let [{:keys [imgs time]} @state01]
        [:div {:style {:height "80vh"
                       :position :relative
                       :margin "10vh"
                       :max-width (max-size 0)
                       :max-height (max-size 1)
                       :background-color "white"}}
         (u/log "REND")
         [Canvas {:width "100%" 
                  :height "100%"
                  :anim anim!
                  :on-resize resize!
                  :anim? true}]]))))


;; sketches --------------------------------------------------------------------

(def sketches
  [{:compo sketch01}])
