(defproject coveralls-status-hook "0.1.0-SNAPSHOT"
  :description "Coveralls Webhook to Github Status Bridge"
  :url "http://coveralls-status-hook.herokuapp.com"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [environ "0.5.0"]
                 [tentacles "0.3.0"]]
  :plugins [[environ/environ.lein "0.2.1"]
            [lein-ring "0.8.13"]]
  :ring {:handler coveralls-status-hook.core.handler/app}
  :hooks [environ.leiningen.hooks]
  :uberjar-name "coveralls-status-hook-standalone.jar"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}
   :production {:env {:production true}}}
  :main coveralls-status-hook.core.handler
  )
