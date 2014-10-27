(ns mjbot.core
  (:use mjbot.parse
        mjbot.config))

(defn finish-up []
  (reset! search-more false)
  (println "Finishing battle"))

(defn switch-debugging []
  (reset! debugging (not @debugging)))