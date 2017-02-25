(ns sketches.core
  (:require
   [sketches.global :as g]
   [reagent.core :as r]
   [sketches.book01 :as b1]
   [sketches.utils :as u]))


(defn on-window-resize []
  (reset! g/window-size (u/inner-size js/window)))


(defn Page []
  [:div 
   (for [{:keys [compo]} b1/sketches]
     [compo])])

(defn main []
  (r/render [Page]
            (.getElementById js/document "app")))

(defn init []
  (main) 
  (set! (.-onresize js/window) on-window-resize)
  (on-window-resize))

