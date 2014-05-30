(ns heroku-data.core
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core :refer :all]))

(defn get-heroku-data [path]
  (let [options {:timeout     30000                         ; ms -- 30 seconds!
                 :oauth-token "20ff0356-ca01-48f6-b8da-d6189a33d61b"
                 :headers     {"Accept: application/vnd.heroku+json; version=3"
                                "Content-type: application/json"}}
        api-url "https://api.heroku.com/"
        {:keys [body error] :as resp} @(http/get (str api-url path) options)]
    (if error
      (println "Failed, exception: " error)
      (parse-string body true))))

(def org-path "/organizations/")

(def orgs (get-heroku-data org-path))

(def apps-per-orgs (map #(get-heroku-data (str org-path (:name %) "/apps")) orgs))

; apply concat to peel away any enclosing sequences
(def app-names (map #(select-keys % [:name :organization]) (apply concat apps-per-orgs)))

(defn get-app-env-vars [app]
  (let [env-vars (get-heroku-data (str "/apps/" (:name app) "/config-vars"))]
    (vector app env-vars)))

(defn env-app-vars [] (map #(get-app-env-vars %) app-names))

; TODO ... store it in mongo for further queries





