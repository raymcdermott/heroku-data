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

(def orgs (get-heroku-data org-path))

(def org-path "/organizations/")

(def orgs (get-heroku-data org-path))

(def apps-per-orgs (map #(get-heroku-data (str org-path (:name %) "/apps")) orgs))

; apply concat to peel away any enclosing sequences
(def app-names (map #(select-keys % [:name :organization]) (apply concat apps-per-orgs)))

; TODO -- return the app with the vars
(defn get-app-env-vars [app]
  (get-heroku-data (str "/apps/" (:name app) "/config-vars")))

(defn env-app-vars [] (map #(get-app-env-vars %) app-names))

(def some-json "{\"MONGOLAB_URI\":\"mongodb://heroku_app22930768_A:KKgliGwPIzbQkMZmWSdsVkFczXcybksX@ds027280-a0.mongolab.com:27280,ds027280-a1.mongolab.com:27280/heroku_app22930768\",\"NEW_RELIC_LICENSE_KEY\":\"7098d10fbf70572dc7da6736964b9b77e49ac164\",\"NEW_RELIC_LOG\":\"stdout\",\"LOADERIO_API_KEY\":\"8ace9ef99a8946bb01c2e40c6f852557\"}")



; TODO add another JSON entry with the app name to the front via a fn

; TODO need to combine the env-vars with the app / org data
; TODO ... store it in mongo for further queries





