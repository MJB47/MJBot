(ns mjbot.data.data
  (:require [clojure.data.json :as json]))

(def data-path "src/mjbot/data/")

; loads a lazy-seq
(defn get-data [file]
  (json/read-str (slurp (str data-path file)) :key-fn keyword))

; 0 = 1x, 1 = 2x, 2 = 0.5x, 3 = 0x
(def types
  (get-data "types.json"))

(defn type-to-eff [t]
  (get {0 1 
        1 2 
        2 0.5 
        3 0} t))

(def moves
  (get-data "moves.json"))

(def pokedex
  (get-data "pokedex.json"))

(def immunity-abilities
  {:levitate :Ground
   :sapsipper :Grass
   :flashfire :Fire
   :waterabsorb :Water
   :voltabsorb :Electric
   :lightningrod :Electric
   :dryskin :Water
   })

(def trash-talk
  ["Its fine, I literally have all day" 
   "No really, I have nothing better to do" 
   "Take your time, wouldn't want to lose because of a missclick now would we?" 
   "Little bit of tortoise and hare going on here it seems" 
   "WOAH slow down there, there might be an accident" 
   "Are you lagging, I assume you are lagging..." 
   "Please say I havn't DC'd..." 
   "Im flattered you need so much thinking time to beat me"])