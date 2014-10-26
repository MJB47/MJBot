(ns mjbot.battle
  (:require [clojure.string :as string]))

(defn find-battle []
  (let [team "/utm"
        tier "/search randombattle"]
    (string/join "\n" [team tier])))

(defn start-timer []
  "/timer")

(defn select-move [opts]
  (let [moves (:moves (get (:active opts) 0))]
    (str "/choose move " (:move (rand-nth moves)) "|" (:rqid opts) "\n" (start-timer))))

(defn get-next-poke [pokemon rqid i]
  (if (first pokemon)
    (if-not (= (subs (:condition (first pokemon)) 0 1) "0")
      (str "/choose switch " i "|" rqid)
      (get-next-poke (rest pokemon) rqid (inc i)))))

(defn switch [opts rqid]
  (let [pokemon (:pokemon opts)]
    (get-next-poke (rest pokemon) rqid 2))) ; first poke is always the one that just died/switched

(defn play-turn [opts]
  (cond
    (:active opts)
    	(select-move opts)
    (:forceSwitch opts)
    	(switch (:side opts) (:rqid opts))
    ))