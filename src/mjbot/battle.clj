(ns mjbot.battle
  (:use mjbot.data.data)
  (:require [clojure.string :as string]))

(def wins (atom 0))
(def losses (atom 0))

(defn update-score [me?]
  (if me? (swap! wins inc) (swap! losses inc)))

(defn find-battle []
  (let [team "/utm"
        tier "/search randombattle"]
    (string/join "\n" [team tier])))

(defn start-timer []
  "/timer")

(defn mega-evo? [side]
  (if (:canMegaEvo (first (:pokemon side))) ;who wrote this code ps-side??
    " mega"))

(defn select-move [opts]
  (let [moves (:moves (get (:active opts) 0))]
    (str "/choose move " (:move (rand-nth moves)) (mega-evo? (:side opts)) "|" (:rqid opts) "\n" (start-timer))))

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