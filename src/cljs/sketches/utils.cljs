(ns sketches.utils
  (:require
   [clojure.core.matrix :refer [add sub mul div]]
   [ajax.core :as aj]
   [promesa.core :as p :include-macros true]))

(def ajax-defaults
  {:method :get
   :format (aj/transit-request-format)
   :response-format (aj/transit-response-format)})

(defn ajax [opts]
  (p/promise
   (fn [resolve reject]
     (let [handler (fn [[ok response]]
                     ((if ok resolve reject)
                      response))]
       (aj/ajax-request (merge {:handler handler} ajax-defaults opts))))))

(defn log [& xs]
  (apply js/console.log xs))

(defn log2 [& xs]
  (apply js/console.log (map prn-str xs)))

(defn get-real-size [el]
  (let [bb (.getBoundingClientRect el)]
    [(int (.-width bb)) (int (.-height bb))]))

(defn ctx2d [el]
  (.getContext el "2d"))

(defn req-anim [f]
  (.requestAnimationFrame js/window f))

(defn draw-image
  ([ctx img]
   (.drawImage ctx img 0 0))
  
  ([ctx img source-frame]
   (draw-image ctx img source-frame [[0 0] (source-frame 1)]))

  ([ctx img source-frame target-frame]
   (let [[[a b] [c d]] source-frame
         [[e f] [g h]] target-frame] 
     (.drawImage ctx img a b c d e f g h))))

(defn get-size [x]
  [(.-width x) (.-height x)])

(defn mk-elem [name]
  (.createElement js/document name))

(def url-encode #(.encodeURIComponent js/window %))

(defn params->str [params]
  (if-not params
    ""
    (str "?" (->> params
                  (map (fn [[k v]]
                         (str (url-encode (name k)) "=" (url-encode v))))
                  (interpose "&")
                  (apply str)))))

(defn load-img [url & [params]]
  (let [el (mk-elem "img")
        url (str url (params->str params))]
    (p/promise (fn [resolve reject]
                 (doto el
                   (aset "crossOrigin" "Anonymous")
                   (aset "onload" #(resolve el))
                   (aset "onerror" reject)
                   (aset "src" url))))))

(defn inner-size [el]
  [(.-innerWidth el) (.-innerHeight el)])


;; taken from clojure.math.combinatorics
(defn cart
  "All the ways to take one item from each sequence"
  [seqs]
  (let [v-original-seqs (vec seqs)
        step
        (fn step [v-seqs]
          (let [increment
                (fn [v-seqs]
                  (loop [i (dec (count v-seqs)), v-seqs v-seqs]
                    (if (= i -1) nil
                        (if-let [rst (next (v-seqs i))]
                          (assoc v-seqs i rst)
                          (recur (dec i) (assoc v-seqs i (v-original-seqs i)))))))]
            (when v-seqs
              (cons (map first v-seqs)
                    (lazy-seq (step (increment v-seqs)))))))]
    (when (every? seq seqs)
      (lazy-seq (step v-original-seqs)))))
