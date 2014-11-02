(ns mjbot.util
  (:require [clojure.string :as string]))

(defn string-to-id [poke]
  (string/lower-case (string/replace poke #"[^\p{L}\p{Nd}]+" "")))

(defn get-poke-from-details [data]
  (keyword (string-to-id (get (string/split data #",") 0))))