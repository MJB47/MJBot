(ns mjbot.battle
  (:use mjbot.data.data
        mjbot.util
        [mjbot.config :only [current-tier teams]])
  (:require [clojure.string :as string]
            [mjbot.config :as config]))

(def wins (atom 0))
(def losses (atom 0))

(def who-am-i (atom nil))
(def opp-poke (atom nil)) ;why isnt this given with all the other information from ps??????
(def opp-status (atom {}))

(def last-request (atom nil)) ;bad hack

(defn set-who-am-i [me]
  (reset! who-am-i me))

(defn set-opp-poke [poke]
  (reset! opp-poke poke))

(defn reset-state []
  (reset! who-am-i nil)
  (reset! opp-poke nil)
  (reset! opp-status {}))

(defn update-score [me?]
  (if me? (swap! wins inc) (swap! losses inc)))

(defn find-battle []
  (let [team (str "/utm " (rand-nth ((keyword @current-tier) teams)))
        tier (str "/search " @current-tier)]
    (string/join "\n" [team tier])))

(defn psychological-warfare []
  (if @config/bm? (rand-nth trash-talk)))

(defn start-timer []
  "/timer")

(defn active-poke [side]
  (get-poke-from-details (:details (first (:pokemon side)))))

;assume sleep clause is always active for now
(defn sleep-clause []
  (loop [pokes @opp-status]
    (if (seq pokes)
      (if (= "slp" (get (first pokes) 1))
        true
        (recur (rest pokes))))))

(defn move-status [move-data]
  (if-not (or ((keyword @opp-poke) @opp-status) (sleep-clause))
    121 ;more than a neutral focus blast or w/e
    0))

(defn move-power [move]
  (let [move-data ((keyword move) moves)]
    (if (:status move-data)
      (move-status move-data)
      (* 
        (:basePower move-data) 
        (or (:multiHit move-data) 1))))) ;lets be optimistic

(defn poke-type [poke i];0 or 1
  (keyword (get (:types (poke pokedex)) i)))

(defn off-effectiveness [type poke]
  (if (= type :Neutral)
    1
    (* 
      (type-to-eff (or (type (:damageTaken ((poke-type poke 0) types))) 0))
      (if (poke-type poke 1) (type-to-eff (or (type (:damageTaken ((poke-type poke 1) types))) 0)) 1))))

;takes move id
(defn move-type [move]
  (let [move (keyword move)]
    (if (:status (move moves))
      (if (zero? (off-effectiveness (keyword (:status (move moves))) @opp-poke))
        (keyword (:status (move moves)))
        (if (zero? (off-effectiveness (keyword (:type (move moves))) @opp-poke))
          (keyword (:type (move moves)))
          :Neutral))
      (keyword (:type (move moves))))))

(defn stab [type side]
  (if (or (= type (poke-type (active-poke side) 0)) (= type (poke-type (active-poke side) 1)))
    1.5
    1))

; for some reason ps gives the id of hidden power to be exactly "hiddenpower" for the active poke, but
; "hiddenpowertypedamage" from side... who wrote this?
(defn full-hidden-power-id [side]
  (let [moves (:moves (first (:pokemon side)))]
    (loop [m moves]
      (if (.startsWith (first m) "hiddenpower")
        (first m)
        (recur (rest m))))))

(defn off-power [original-move poke side]
  (let [move (if (.startsWith original-move "hiddenpower") "hiddenpower" original-move)
        type (if (= move "hiddenpower") 
               (if-not (= original-move "hiddenpower")
                 (type-of-hidden-power original-move)
                 (type-of-hidden-power (full-hidden-power-id side)))
               (move-type move))]
    (*
      (move-power move)
      (off-effectiveness type poke)
      (stab type side))))

(defn best-move-power [move-ids side]
  (loop [m move-ids
         best-name nil
         best-power 0]
    (if (seq m)
      (let [power (off-power (first m) @opp-poke side)]
        (if (< best-power power)
              (recur (rest m)
                     (first m)
                     power)
              (recur (rest m) best-name best-power)))
      {:move best-name :power best-power})))

(defn fainted? [pokemon]
  (= (subs (:condition (first pokemon)) 0 1) "0"))

(defn get-next-poke [pokemon rqid i]
  (if (seq pokemon)
    (if-not (fainted? pokemon)
      (str "switch " i "|" rqid)
      (get-next-poke (rest pokemon) rqid (inc i)))))

(defn good-enough? [power]
  (>= 60 (:power power)))

(defn good-switch [pokemon rqid i]
  (loop [p pokemon
         i i
         best nil
         best-power 0]
    (if (seq p)
      (if-not (fainted? p)
        (let [power (best-move-power (:moves (first p)) {:pokemon p})]
          (do 
            (println best-power power)
            (if-not (good-enough? power)
              (if (< best-power (:power power))
                (recur (rest p)
                       (inc i)
                       i
                       (:power power))
                (recur (rest p) (inc i) best best-power))
              (recur (rest p) (inc i) best best-power))))
        (recur (rest p) (inc i) best best-power))
      (if best
        (do (println "best: " best " - " best-power) (str "switch " best "|" rqid))
        (get-next-poke pokemon rqid 2)))))

(defn get-move-ids [cmoves]
  (loop [r cmoves
         t []]
    (if (seq r)
      (if (:disabled (first r))
          (recur (rest r)
                 t)
          (recur (rest r)
                 (conj t (:id (first r)))))
      t)))

(defn best-move [cmoves side]
  (let [best-attack (best-move-power (get-move-ids cmoves) side)
        best-name (:move best-attack)
        pokemon (rest (:pokemon (:side @last-request)))]
    (if (good-enough? best-attack)
      (or 
        (good-switch pokemon (:rqid @last-request) 2)
        (str "move " (or best-name (:move (rand-nth cmoves)))))
      (str "move " (or best-name (:move (rand-nth cmoves)))))))

(defn mega-evo? [side]
  (if (:canMegaEvo (first (:pokemon side))) ;who wrote this code ps-side??
    " mega"))

(defn select-move [opts]
  (let [cmoves (:moves (get (:active opts) 0))
        side (:side opts)]
    (str "/choose " (if @opp-poke (best-move cmoves side) (:move (rand-nth cmoves))) (mega-evo? (:side opts)) "|" (:rqid opts) "\n" (start-timer))))

(defn switch [opts rqid]
  (let [pokemon (:pokemon opts)]
    (str "/choose " (good-switch (rest pokemon) rqid 2)))) ; first poke is always the one that just died/switched

(defn play-turn [opts]
  (cond
    (:active opts)
    	(select-move opts)
    (:forceSwitch opts)
    	(switch (:side opts) (:rqid opts))
    (:wait opts)
    	(psychological-warfare)
    (:teamPreview opts)
    	(str "/team 1|" (:rqid opts) "\n" (start-timer))))