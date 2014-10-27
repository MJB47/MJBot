(ns mjbot.core
  (:use mjbot.parse
        mjbot.config))

(defn finish-up []
  (reset! search-more false)
  (println "Finishing battle"))