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
          	(do 
            	(send-msg room "Good Game")
             	(send-msg "" (str "/leave " room))
              (send-msg "" (find-battle)))
          (= type "updateuser")
          	(send-msg "" (find-battle)))))))

(defn parse-msg [msg]
  (if config/debugging (prn msg))
  (if (= ">" (subs msg 0 1))
    (let [smsg (string/split (subs msg 1) #"\r\n|\r|\n") ; i think ps only supports \n, but w/e
          room (first smsg)]
      (doseq [line (rest smsg)] (parse-line room line)))
    (parse-line "" msg)))

(def socket
  (ws/connect
    "ws://sim.smogon.com:8000/showdown/websocket"
    :on-receive #(parse-msg %)))