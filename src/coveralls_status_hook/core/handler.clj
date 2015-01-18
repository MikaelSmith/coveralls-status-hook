(ns coveralls-status-hook.core.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [tentacles.repos :as repos]
            [clojure.string :as str]))

(def oauth_token (System/getenv "GITHUB_OAUTH_TOKEN"))
(def change_threshold (Double/parseDouble (or (System/getenv "COVERALLS_FAILURE") "-1.0")))

(defn update-github-status [request]
  (let [[user repo] (str/split (get-in request [:params :repo_name]) #"/")
        sha (get-in request [:params :commit_sha])
        change (get-in request [:params :coverage_change])
        changenum (Double/parseDouble change)
        target_url (get-in request [:params :url])
        branch (get-in request [:params :branch])
        desc (str "Coverage "
                  (cond
                    (< changenum 0.0) (str "has improved (" change ")")
                    (> changenum 0.0) (str "has declined (" change ")")
                    :else "remained the same")
                  " for " sha " on " branch ".")
        success (if (< changenum change_threshold) "failure" "success")
        context "continuous-integration/coveralls"
        ]
    (repos/create-status user repo sha
      {:oauth-token oauth_token
       :state success
       :target-url target_url
       :description desc
       :context context})
    )
  "done"
  )

(defroutes app-routes
  (GET "/ping" [] "pong")
  (POST (str "/" (System/getenv "WEBHOOK_PATH")) [] update-github-status)
  (route/not-found "Not Found"))

; Anti-forgery (CSRF) protection is disabled; no cookies are used by the app.
(def app
  (wrap-defaults app-routes (assoc-in site-defaults [:security :anti-forgery] false)))
