(ns mjbot.core
  (:use mjbot.parse
        mjbot.config
        [mjbot.battle :only [find-battle]]))

(defn start-search []
  (send-msg "" (find-battle)))

(defn finish []
  (reset! search-more? false)
  (println "Finishing battle"))

(defn switch-debugging []
  (reset! debugging? (not @debugging?)))

(defn change-tier [tier]
  (reset! current-tier tier))