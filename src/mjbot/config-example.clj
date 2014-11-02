(ns mjbot.config)

(def user "MJBot")
(def pass "")
(def debugging? (atom false))
(def search-more? (atom true))
(def bm? (atom false))

(def current-tier (atom "randombattle"))
(def teams {:randombattle [""]})