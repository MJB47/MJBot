(ns mjbot.parse
  (:use mjbot.battle)
  (:require [clojure.string :as string]
            [org.httpkit.client :as http]
            [mjbot.config :as config]
            [clojure.data.json :as json]
            [gniazdo.core :as ws]))

(declare socket)

(defn send-msg [room msg]
  (ws/send-msg socket (str room "|" msg))
  (println "> " msg))

(defn parse-json [json]
  (json/read-str json :key-fn keyword))

(defn handle-resp [resp]
  (let [data (parse-json (subs resp 1))
        assertion (:assertion data)]
    (send-msg "" (str "/trn " config/user ",0," assertion))))

(defn login [keyid challenge]
  (let [url "http://play.pokemonshowdown.com/action.php"
        options {:form-params {:act "login" :name config/user :pass config/pass :challengekeyid keyid :challenge challenge}}]
    (http/post url options
      (fn [{:keys [status headers body error]}]
        (if error
          (println "Failed to login, exception is: " error)
          (handle-resp body))))))

(defn handle-win [room smsg]
  (send-msg room "Good Game")
 	(send-msg "" (str "/leave " room))
  (update-score (if (= (nth smsg 2) config/user) true false))
  (reset-state)
  (println (str "Score so far this session: " @wins "/" @losses))
  (if @config/search-more? (send-msg "" (find-battle)) (System/exit 0)))

(defn get-poke-from-switch [data]
  (nth (string/split data #",") 0))

(defn parse-line [room msg]
  (if-not (or (= msg "") (= msg "|"))
    (if (= "|" (subs msg 0 1))
      (let [smsg (string/split msg #"\|")
            type (nth smsg 1)]
        (cond
          (= type "challstr")
          	(login (nth smsg 2) (nth smsg 3))
          (= type "request")
          	(send-msg room (play-turn (parse-json (nth smsg 2))))
          (= type "init")
          	(if (= (nth smsg 2) "battle") (send-msg room "Good Luck and Have Fun"))
          (= type "win")
          	(handle-win room smsg)
          (= type "updateuser")
          	(if (= (nth smsg 2) config/user) (send-msg "" (find-battle)))
          (= type "player")
          	(if (>= (count smsg) 4) (if (= (nth smsg 3) config/user) (set-who-am-i (nth smsg 2))))
           ;if its a switch message, check if its the opponent
          (and (= type "switch") (not (= (subs (nth smsg 2) 0 2) @who-am-i))) ; this is ugly as hell
          	(reset! opp-poke (get-poke-from-switch (nth smsg 3))))))))

(defn parse-msg [msg]
  (if @config/debugging? (prn msg))
  (if (= ">" (subs msg 0 1))
    (let [smsg (string/split (subs msg 1) #"\r\n|\r|\n") ; i think ps only supports \n, but w/e
          room (first smsg)]
      (doseq [line (rest smsg)] (parse-line room line)))
    (parse-line "" msg)))

(def socket
  (ws/connect
    "ws://sim.smogon.com:8000/showdown/websocket"
    :on-receive #(parse-msg %)))