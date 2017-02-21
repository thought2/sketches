(ns sketches.core
    (:require [reagent.core :as r]))

(defn Page []
  [:div "hello sketches!"])

(defn main []
  (r/render [Page]
            (.getElementById js/document "app")))

(defn init []
  (main))
