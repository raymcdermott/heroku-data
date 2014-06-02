(ns heroku-data.core
  (:require [clojure.set :as s])
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core :refer :all])
  (:require [monger.core :as mg]
            [monger.collection :as mc]))

(assert (not-empty (System/getenv "HEROKU_API_TOKEN")))
;(assert (not-empty (System/getenv "MONGO_URL")))

(defn get-heroku-data [path]
  (let [options {:timeout     30000                         ; ms -- 30 seconds!
                 :oauth-token (System/getenv "HEROKU_API_TOKEN")
                 :headers     {"Accept: application/vnd.heroku+json; version=3"
                                "Content-type: application/json"}}
        {:keys [body error]} @(http/get (str "https://api.heroku.com/" path) options)]
    (if error
      (println "Failed, exception: " error)
      (parse-string body true))))

(defn get-env-vars [app-name]
  (let [env-vars (get-heroku-data (str "/apps/" app-name "/config-vars"))]
    (hash-map :app app-name :env env-vars)))

(defn app-differ [app1 app2]
  (let [diff (s/difference (set app1) (set app2))]
    (hash-map :apps (str (:name app1) " vs " (:name app2)) :diff diff )))

(defn env-differ [app1 app2]
  (let [diff (s/difference (set (:env app1)) (set (:env app2)))]
    (hash-map :apps (str (:app app1) " vs " (:app app2)) :diff diff )))

(defn env-variance [app-to-compare & other-apps]
  (let [from-app (get-env-vars app-to-compare)
        comparative-apps (map #(get-env-vars %) other-apps)
        diff (map #(env-differ % from-app) comparative-apps)]
    (println app-to-compare " has this environment configuration: ")
    (println (set (:env from-app)))
    (println diff)))

(defn get-app [app-name]
  (get-heroku-data (str "/apps/" app-name)))

(defn app-differ [app1 app2]
  (let [diff (s/difference (set app1) (set app2))]
    (hash-map :apps (str (:name app1) " vs " (:name app2)) :diff diff )))

(defn app-variance [app-to-compare & other-apps]
  (let [from-app (get-app app-to-compare)
        comparative-apps (map #(get-app %) other-apps)
        diff (map #(app-differ % from-app) comparative-apps)]
    (println app-to-compare " has this application configuration: ")
    (println from-app)
    (println diff)))

; TODO - find a nice way to filter away common differences
; dissoc does not work on sets ;-)

; TODO - write a main program to be executed from the command line


;---> Scratch pad functions

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

(defn all-org-app-env []
  "Gets config data, for all apps for all orgs and saves it (per app) to mongoDB"
  (let [apps (apps-all-orgs)
        data (map #(get-app-env-vars %) apps)]
    (map save-to-mongo data)))

(defn org-app-env [organization]
  "Gets config data, for all apps for the given organization"
  (let [apps (apps-per-org organization)
        data (map #(get-app-env-vars %) apps)]
    data))

(defn forbidden-apps [apps]
  (filter #(= "forbidden" (get-in % [:env :id])) apps))

(defn allowed-apps [organization]
  (let [org-apps (set (apps-per-org organization))
        forbidden (set (forbidden-apps org-apps))]
    (s/difference org-apps forbidden)))

