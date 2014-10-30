(ns mjbot.util
  (:require [clojure.string :as string]))

(defn poke-to-id [poke]
  (string/lower-case (string/replace poke #"[^\p{L}\p{Nd}]+" "")))

(defn get-poke-from-details [data]
  (keyword (poke-to-id (get (string/split data #",") 0))))