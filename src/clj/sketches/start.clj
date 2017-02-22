(ns sketches.start
    (:gen-class)
    (:require [sketches.server          :refer [handler]]
              [ring.adapter.jetty       :refer [run-jetty]]))

(defn -main [& [port]]
  (run-jetty handler {:port (Integer. (or port (System/getenv "PORT") 5000))}))
