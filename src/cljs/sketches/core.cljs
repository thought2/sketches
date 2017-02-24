(ns sketches.core
  (:require [reagent.core :as r]
            [sketches.book01 :as b1]))

(defn Page []
  [:div 
   (for [{:keys [compo]} b1/sketches]
     [compo])])

(defn main []
  (r/render [Page]
            (.getElementById js/document "app")))

(defn init []
  (main))

