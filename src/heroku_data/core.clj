(ns heroku-data.core
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core :refer :all])
  (:require [monger.core :as mg]
            [monger.conversion :as mco]
            [monger.collection :as mcl])
  (:import org.bson.types.ObjectId))

(assert (not-empty (System/getenv "HEROKU_API_TOKEN")))

(defn get-heroku-data [path]
  (let [options {:timeout     30000                         ; ms -- 30 seconds!
                 :oauth-token (System/getenv "HEROKU_API_TOKEN")
                 :headers     {"Accept: application/vnd.heroku+json; version=3"
                                "Content-type: application/json"}}
        {:keys [body error]} @(http/get (str "https://api.heroku.com/" path) options)]
    (if error
      (println "Failed, exception: " error)
      (parse-string body true))))

(def org-path "/organizations/")

(def orgs (get-heroku-data org-path))

(def apps-per-orgs (apply concat (map #(get-heroku-data (str org-path (:name %) "/apps")) orgs)))

(defn get-app-env-vars [app]
  (let [env-vars (get-heroku-data (str "/apps/" (:name app) "/config-vars"))]
    (hash-map :app app :env env-vars)))

(defn save-to-mongo [configuration-data]
  (let [uri (System/getenv "MONGO_URL")
        {:keys [conn db]} (mg/connect-via-uri "mongodb://127.0.0.1/monger-test")
        coll "org-app-env"
        data (mco/to-db-object configuration-data)]
    (mcl/insert db coll data)
    (mg/disconnect conn)))

; this gets data for all environments, for all apps for all orgs and saves it to mongoDB
(defn org-app-env []
  (let [data (map #(get-app-env-vars %) apps-per-orgs)]
    (map save-to-mongo data)))

