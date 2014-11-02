(ns mjbot.parse
  (:use mjbot.battle
        mjbot.util)
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
  (update-score (if (= (get smsg 2) config/user) true false))
  (reset-state)
  (println (str "Score so far this session: " @wins "/" @losses))
  (if @config/search-more? (send-msg "" (find-battle)) (System/exit 0)))

(defn handle-request [room request]
  (if (:active request)
    (reset! last-request request)
    (send-msg room (play-turn request))))

(defn handle-switch [room smsg]
  (reset! opp-poke (get-poke-from-details (get smsg 3))))

(defn handle-opp-status [smsg]
  (if (not (= (subs (get smsg 2) 0 2) @who-am-i))
    (swap! opp-status assoc {(keyword (string-to-id (subs (get smsg 2) 5))) (get smsg 3)})))

(defn parse-line [room msg]
  (if-not (or (= msg "") (= msg "|"))
    (if (= "|" (subs msg 0 1))
      (let [smsg (string/split msg #"\|")
            type (get smsg 1)]
        (cond
          (= type "challstr")
          	(login (get smsg 2) (get smsg 3))
          (= type "request")
          	(handle-request room (parse-json (get smsg 2)))
          (= type "init")
          	(if (= (get smsg 2) "battle") (send-msg room "Good Luck and Have Fun"))
          (= type "win")
          	(handle-win room smsg)
          (= type "updateuser")
          	(if (= (get smsg 2) config/user) (send-msg "" (find-battle)))
          (= type "player")
          	(if (>= (count smsg) 4) (if (= (get smsg 3) config/user) (set-who-am-i (get smsg 2))))
           ;if its a switch message, check if its the opponent
          (and (= type (or "switch" "detailschange" "drag")) (not (= (subs (get smsg 2) 0 2) @who-am-i))) ; this is ugly as hell
          	(handle-switch room smsg)
          (and (= type "faint") (not (= (subs (get smsg 2) 0 2) @who-am-i))) ;temporary until refactor
          	(reset! opp-poke nil)
          (= type "-status")
          	(handle-opp-status smsg)
          (= type "-curestatus")
          	(swap! opp-status dissoc (keyword (string-to-id (subs (get smsg 2) 5))))
          (= type "-cureteam")
          	(reset! opp-status {})
          (= type "turn")
          	(send-msg room (play-turn @last-request))
          )))))

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