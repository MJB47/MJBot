(ns mjbot.battle
  (:use mjbot.data.data)
  (:require [clojure.string :as string]
            [mjbot.config :as config]))

(def wins (atom 0))
(def losses (atom 0))

(def who-am-i (atom nil))
(def opp-poke (atom nil)) ;why isnt this given with all the other information from ps??????

(defn set-who-am-i [me]
  (reset! who-am-i me))

(defn set-opp-poke [poke]
  (reset! opp-poke poke))

(defn reset-state []
  (reset! who-am-i nil)
  (reset! opp-poke nil))

(defn update-score [me?]
  (if me? (swap! wins inc) (swap! losses inc)))

(defn find-battle []
  (let [team "/utm"
        tier "/search randombattle"]
    (string/join "\n" [team tier])))

(defn psychological-warfare []
  (if @config/bm? (rand-nth trash-talk)))

(defn start-timer []
  "/timer")

;takes move id
(defn move-type [move]
  (keyword (:type ((keyword move) moves))))

(defn move-power [move]
  (* 
    (:basePower ((keyword move) moves)) 
    (or (:multiHit ((keyword move) moves)) 1))) ;lets be optimistic

;returns nil if its not a status move
(defn move-status [move]
  (:status ((keyword move) moves)))

(defn poke-type [poke i];0 or 1
  (keyword (get (:types (poke pokedex)) i)))

(defn off-effectiveness [type poke]
  (* 
    (type-to-eff (type (:damageTaken ((poke-type poke 0) types))))
    (if (poke-type poke 1) (type-to-eff (type (:damageTaken ((poke-type poke 1) types)))) 1)))

(defn off-power [move poke]
  (*
    (move-power move)
    (off-effectiveness (move-type move) poke)))

(defn best-move [cmoves]
  (loop [m cmoves
         best-name nil
         best-power 0]
    (if (seq m)
      (if (< best-power (off-power (:id (first m)) @opp-poke))
        (recur (rest m)
               (:move (first m))
               (off-power (:id (first m)) @opp-poke))
        (recur (rest m) best-name best-power))
      (or best-name (rand-nth cmoves)))))

(defn mega-evo? [side]
  (if (:canMegaEvo (first (:pokemon side))) ;who wrote this code ps-side??
    " mega"))

(defn select-move [opts]
  (let [cmoves (:moves (get (:active opts) 0))]
    (str "/choose move " (if @opp-poke (best-move cmoves) (:move (rand-nth cmoves))) (mega-evo? (:side opts)) "|" (:rqid opts) "\n" (start-timer))))

(defn get-next-poke [pokemon rqid i]
  (if (seq pokemon)
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
    (:wait opts)
    	(psychological-warfare)))