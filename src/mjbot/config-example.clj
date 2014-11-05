(ns mjbot.config)

(def user "MJBot")
(def pass "")
(def master "") ; username of the bot's owner. for debugging right now
(def debugging? (atom false))
(def search-more? (atom true))
(def bm? (atom false))

(def current-tier (atom "randombattle"))
(def teams {:randombattle [""]})