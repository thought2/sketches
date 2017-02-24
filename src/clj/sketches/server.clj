(ns sketches.server
  (:require
   [sketches.imgs :as i]
   [compojure.route :as route]
   [compojure.core :refer :all]
   [clojure.java.io :as io]
   [clojure.string :as s]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.transit :refer
    [wrap-transit-params
     wrap-transit-response
     wrap-transit-body]]
   [ring.util.response :refer [response not-found]]))

(defn get-file-list [dir]
  (let [root (str (io/resource ""))] 
    (->> (io/resource dir)
         io/file
         .listFiles
         (filter #(.isFile %))
         (map (comp #(str dir "/" %)
                    last
                    #(s/split % #"/")
                    str)))))

(defn jpeg-response [image-data]
  (-> image-data
      (ring.util.response/response)
      (ring.util.response/content-type "image/jpeg")))

(defn serve-img [path width] 
  (-> (io/resource path)
      io/file
      i/read-img
      (i/scale-down [(Integer. width) (Integer. width)]) 
      i/img->byte-array      
      jpeg-response))

(defroutes main-routes
  (GET "/" _ (io/resource "index.html"))
  (POST "/ls" [dir] (response (get-file-list dir)))
  (GET "/img" [path width] (serve-img path width))   

  (route/resources "/" {:root ""}))

(defn logger [handler]
  (fn [req]
    (prn req)
    (handler req)))

(def handler
  (-> main-routes
      (logger)
      #_(wrap-transit-body {:keywords? true :opts {}})
      (wrap-transit-params)
      (wrap-transit-response)
      wrap-params
      #_(wrap-restful-format)
      (logger)))
