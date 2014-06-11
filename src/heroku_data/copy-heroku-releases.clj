(ns heroku-data.copy-heroku-releases
  (:require [clojure.set :as s])
  (:require [org.httpkit.client :as http])
  (:require [cheshire.core :refer :all])
  (:require [monger.core :as mg]
            [monger.collection :as mc]))

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

(defn post-heroku-data [path json-data]
  (let [options {:timeout      30000                        ; ms -- 30 seconds!
                 :oauth-token  (System/getenv "HEROKU_API_TOKEN")
                 :headers      {"Accept: application/vnd.heroku+json; version=3"
                                 "Content-type: application/json"}
                 :query-params json-data}
        {:keys [body error]} @(http/post (str "https://api.heroku.com/" path) options)]
    (if error
      (println "Failed, exception: " error)
      (parse-string body true))))


(defn apps-per-org [organization]
  (get-heroku-data (str "/organizations/" organization "/apps")))

;(target-apps "tme-web-preview" #"bamboo-.*")
;(target-apps "tme-web-preview" #".*-[a-z]{2}")
(defn target-apps [organization regex]
  (let [apps (apps-per-org organization)]
    (filter (fn [app] (re-matches regex (:name app))) apps)))


(defn app-releases [app]
  (get-heroku-data (str "/apps/" app "/releases")))

(defn find-last-release [app]
  (let [releases (app-releases app)
        sorted-list (sort-by :version < releases)]
    (last sorted-list)))

(defn find-specific-release [app version]
  (let [releases (app-releases app)]
    (filter #(= version (:version %)) releases)))


(defn copy-release [slug target]
  (println (str "slug: " slug))
  (post-heroku-data (str "/apps/" target "/releases") (generate-string {:slug slug})))

(defn copy-releases [slug targets]
  (doall (map #(copy-release slug %) targets)))


(defn copy-heroku-releases [src-app target-organisation target-regex]
  (let [targets (target-apps target-organisation target-regex)
        release (find-last-release src-app)]
    (copy-releases (get-in release [:slug :id]) targets)))


