(ns heroku-data.core
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core :refer :all]))


(defn get-heroku-data [path options]

  ; TODO -- make these defs into lets (more idiomatic)
  (def options {:timeout     30000                            ; ms -- 30 seconds!
                :oauth-token "20ff0356-ca01-48f6-b8da-d6189a33d61b"
                :headers     {"Accept: application/vnd.heroku+json; version=3"
                               "Content-type: application/json"}})

  (def api-url "https://api.heroku.com/")

  (let [{:keys [body error] :as resp} @(http/get (str api-url path) options)]
    (if error
      (println "Failed, exception: " error)
      body)))

(def org-url "/organizations/")

(def orgs (:name (parse-string (get-heroku-data org-url options) true)))

(def json2map [s] (map #(parse-string % true) s))

(def apps-per-orgs (json2map (map #(get-heroku-data (str org-url % "/apps") options) orgs)))

; apply concat to peel away any enclosing sequences
(def app-names (map #(select-keys % [:name]) (apply concat apps-per-orgs)))

(def env-app-vars (json2map (map #(get-heroku-data (str "/apps/" % "/config-vars") options)
                                 (map :name app-names))))

(def some-json "{\"MONGOLAB_URI\":\"mongodb://heroku_app22930768_A:KKgliGwPIzbQkMZmWSdsVkFczXcybksX@ds027280-a0.mongolab.com:27280,ds027280-a1.mongolab.com:27280/heroku_app22930768\",\"NEW_RELIC_LICENSE_KEY\":\"7098d10fbf70572dc7da6736964b9b77e49ac164\",\"NEW_RELIC_LOG\":\"stdout\",\"LOADERIO_API_KEY\":\"8ace9ef99a8946bb01c2e40c6f852557\"}")

; add another JSON entry with the app name to the front
; sed regex style ;-) like s/^{/{name=val/

; TODO need to combine the env-vars with the app / org data
; TODO ... store it in mongo for further queries






