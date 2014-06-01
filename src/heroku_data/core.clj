(ns heroku-data.core
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core :refer :all])
  (:require [monger.core :as mg]
            [monger.collection :as mc]))

(assert (not-empty (System/getenv "HEROKU_API_TOKEN")))
(assert (not-empty (System/getenv "MONGO_URL")))

(defn get-heroku-data [path]
  (let [options {:timeout     30000                         ; ms -- 30 seconds!
                 :oauth-token (System/getenv "HEROKU_API_TOKEN")
                 :headers     {"Accept: application/vnd.heroku+json; version=3"
                                "Content-type: application/json"}}
        {:keys [body error]} @(http/get (str "https://api.heroku.com/" path) options)]
    (if error
      (println "Failed, exception: " error)
      (parse-string body true))))

(defn apps-per-org [organization]
  (get-heroku-data (str "/organizations/" organization "/apps")))

(defn apps-all-orgs []
  (let [orgs (get-heroku-data "/organizations/")]
    (apply concat (map #(apps-per-org (:name %)) orgs))))

(defn get-app-env-vars [app]
  (let [env-vars (get-heroku-data (str "/apps/" (:name app) "/config-vars"))]
    (hash-map :app app :env env-vars)))

(defn save-to-mongo [configuration-data]
  (let [uri (System/getenv "MONGO_URL")
        {:keys [conn db]} (mg/connect-via-uri uri)]
    (mc/insert-and-return db "orgAppEnv" configuration-data)
    (mg/disconnect conn)))

(defn org-app-env []
  "Gets config data, for all apps for all orgs and saves it (per app) to mongoDB"
  (let [apps (apps-all-orgs)
        data (map #(get-app-env-vars %) apps)]
    (map save-to-mongo data)))