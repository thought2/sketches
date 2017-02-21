(ns sketches.styles
    (:require [garden.def :refer [defrule defstyles]]
              [garden.stylesheet :refer [rule]]))

(defstyles base
  [:body :html :#app {:height "100%"}]
  [:body {:background-color :black}])
