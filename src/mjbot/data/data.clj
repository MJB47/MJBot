(ns mjbot.data.data
  (:require [clojure.data.json :as json]))

(def data-path "src/mjbot/data/")

; loads a lazy-seq
(defn get-data [file]
  (json/read-str (slurp (str data-path file)) :key-fn keyword))

(def types
  (get-data "types.json"))

(def moves
  (get-data "moves.json"))

(def pokedex
  (get-data "pokedex.json"))