(ns frontend.persist-db
   "Backend of DB based graph"
   (:require [frontend.persist-db.browser :as browser]
             [frontend.persist-db.protocol :as protocol]
             [promesa.core :as p]
             [frontend.config :as config]
             [frontend.state :as state]
             [frontend.util :as util]))

(defonce opfs-db (browser/->InBrowser))

 (defn- get-impl
  "Get the actual implementation of PersistentDB"
  []
  opfs-db)

 (defn <list-db []
   (protocol/<list-db (get-impl)))

 (defn <unsafe-delete [repo]
   (protocol/<unsafe-delete (get-impl) repo))

(defn <transact-data [repo tx-data tx-meta]
  (protocol/<transact-data (get-impl) repo tx-data tx-meta))

(defn <export-db
  [repo opts]
  (protocol/<export-db (get-impl) repo opts))

(defn <import-db
  [repo data]
  (protocol/<import-db (get-impl) repo data))

(defn <fetch-init-data
  ([repo]
   (<fetch-init-data repo {}))
  ([repo opts]
   (p/let [ret (protocol/<fetch-initial-data (get-impl) repo opts)]
     (js/console.log "fetch-initial-data" ret)
     ret)))

;; FIXME: limit repo name's length
;; @shuyu Do we still need this?
(defn <new [repo]
  {:pre [(<= (count repo) 56)]}
  (p/let [_ (protocol/<new (get-impl) repo)]
    (<export-db repo {})))

(defn <release-access-handles
  [repo]
  (protocol/<release-access-handles (get-impl) repo))

(defn run-export-periodically!
  []
  (js/setInterval
   (fn []
     (when-let [repo (state/get-current-repo)]
       (when (and (util/electron?) (config/db-based-graph? repo))
         (println :debug :save-db-to-disk repo)
         (<export-db repo {}))))
   ;; every 10 minutes
   (* 10 60 1000)))
