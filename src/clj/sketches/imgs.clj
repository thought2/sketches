(ns sketches.imgs
  (:import javax.imageio.ImageIO
           java.awt.image.BufferedImage
           java.awt.RenderingHints
           java.io.ByteArrayOutputStream
           java.io.ByteArrayInputStream))

(defn read-img [file]
  (ImageIO/read file))

(defn img->byte-array [img]
  (let [output-stream (ByteArrayOutputStream.)]
    (ImageIO/write img "jpg" output-stream)
    (ByteArrayInputStream. (.toByteArray output-stream))))

(defn scale-down [img [width height] & [qual]]
  (let [new-img (BufferedImage. width height (.getType img)) 
        modes {:low    RenderingHints/VALUE_INTERPOLATION_BICUBIC
               :medium RenderingHints/VALUE_INTERPOLATION_BILINEAR
               :high   RenderingHints/VALUE_INTERPOLATION_BICUBIC}
        mode (modes (or qual :medium))]
    (doto (.createGraphics new-img)
      (.setRenderingHint RenderingHints/KEY_INTERPOLATION mode)
      (.drawImage img 0 0 width height nil)
      .dispose)
    new-img))
