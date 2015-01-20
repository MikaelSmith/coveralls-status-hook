(ns coveralls-status-hook.core.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [tentacles.repos :as repos]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(def oauth_token (env :github-oauth-token))
(def change_threshold (Double/parseDouble (or (env :coveralls-failure) "-1.0")))

(defn find-possible-pr
  "Return the parent repo, or nil if the specified sha doesn't exist in the parent."
  [user repo sha]
  (let [request (repos/specific-repo user repo {:oauth-token oauth_token})]
    (log/infof "----- Get parent repo for %s/%s -----" user repo)
    (log/info request)
    (get-in request [:parent :owner :login])
    )
  )

(defn get-commit
  "Return the commit signature of a specific commit"
  [user repo sha]
  (if-let [request (repos/specific-commit user repo sha {:oauth-token oauth_token})]
    (do (log/infof "----- Get commit %s/%s:%s -----" user repo sha)
        (log/info request)
        (request :commit))))

(defn commits=
  "Compare commits from different repos to ensure they're identical."
  [commit1 commit2]
  (= (dissoc commit1 :tree :url)
     (dissoc commit2 :tree :url)))

(defn coverage-description
  "Return a description of the change in code coverage"
  [covered change]
  (str "Coverage "
       (if covered (str "(" covered "%) "))
       (cond
         (< change 0.0) (str "rose " change)
         (> change 0.0) (str "fell " change)
         :else "remained the same")))

(defn update-github-status
  "Post a Github status for Coveralls.io to the commit of the specified repo,
  and the parent repo if found."
  [request]
  (log/info "----- Incoming request -----")
  (log/info request)

  (let [[user repo] (str/split (get-in request [:params :repo_name]) #"/")
        sha (get-in request [:params :commit_sha])
        change (Double/parseDouble (get-in request [:params :coverage_change]))
        target_url (get-in request [:params :url])
        branch (get-in request [:params :branch])
        desc (coverage-description (get-in request [:params :covered_percent]) change)
        success (if (< change change_threshold) "failure" "success")
        context "continuous-integration/coveralls"
        parent (find-possible-pr user repo sha)
        ]
    (log/infof "----- Update status of %s/%s:%s -----" user repo sha)
    (log/info (repos/create-status user repo sha
                                   {:oauth-token oauth_token
                                    :state success
                                    :target-url target_url
                                    :description desc
                                    :context context}))

    (if (and parent (commits= (get-commit user repo sha) (get-commit parent repo sha)))
      (do (log/infof "----- Update status of %s/%s:%s -----" parent repo sha)
          (log/info (repos/create-status parent repo sha
                                         {:oauth-token oauth_token
                                          :state success
                                          :target-url target_url
                                          :description desc
                                          :context context})))
      (log/warn (if parent "Commits didn't match." "No parent found.")))
    )
  "done"
  )

(defroutes app-routes
  (GET "/ping" [] "pong")
  (POST (str "/" (env :webhook-path)) [] update-github-status)
  (route/not-found "Not Found"))

; Disable anti-forgery (CSRF) protection; as the project states, it's not appropriate for web services
(def app
  (wrap-defaults app-routes (assoc-in site-defaults [:security :anti-forgery] false)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

