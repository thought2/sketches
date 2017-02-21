(ns sketches.server
    (:require
     [compojure.route :as route]
     [compojure.core :refer :all]
     [clojure.java.io :as io] 
     [ring.middleware.transit :refer [wrap-transit-params wrap-transit-response]]
     [ring.util.response :refer [response not-found]]))

(defroutes main-routes
  (GET "/" _ (io/resource "index.html"))   
  (route/resources "/" {:root ""}))

(def handler
  (-> main-routes
      (wrap-transit-params)
      (wrap-transit-response)))
