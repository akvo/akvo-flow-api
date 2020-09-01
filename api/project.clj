(defproject org.akvo/flow-api "0.1.0-SNAPSHOT"
  :description "Akvo Flow API"
  :url "https://github.com/akvo/akvo-flow-api"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [com.stuartsierra/component "0.3.1"]
                 [compojure "1.5.1"]
                 [duct "0.8.2"]
                 [environ "1.1.0"]
                 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-jetty-component "0.3.1"]
                 [commons-fileupload "1.3.1"]
                 [org.akvo.flow/akvo-flow "20200901-121115.9ce5653f" :classifier "classes"]
                 [org.akvo/commons "0.4.5" :exclusions [org.clojure/tools.nrepl]]
                 [raven-clj "1.5.0"]
                 [javax.jdo/jdo2-api "2.3-eb"]
                 [com.google.appengine/appengine-tools-sdk "1.9.63"]
                 [com.google.appengine/appengine-remote-api "1.9.63"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.9.63"]
                 [com.google.appengine/appengine-jsr107cache "1.9.63"]
                 [net.sf.jsr107cache/jsr107cache "1.1"]
                 [javax.servlet/servlet-api "2.5"]
                 [org.apache.geronimo.specs/geronimo-jpa_3.0_spec "1.1.1"]
                 [org.postgresql/postgresql "42.2.6"]
                 [com.zaxxer/HikariCP "3.3.1"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [com.google.cloud.sql/postgres-socket-factory "1.0.16"]
                 [clj-http "2.3.0"]
                 [cheshire "5.7.0"]
                 [nrepl/nrepl "0.6.0"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clojure"]
  :plugins [[lein-environ "1.0.3"]
            [jonase/eastwood "0.3.7"]]
  :aliases {"setup"  ["run" "-m" "duct.util.repl/setup"]}
  :profiles
  {:dev  [:project/dev  :profiles/dev]
   :test [:project/test :profiles/test]
   :assemble {:plugins [[com.chartbeat.cljbeat/lein-assemble "0.1.4"]]
              :aot :all
              :omit-source true
              :assemble {:jar {:dest "lib" :uberjar false}
                         :deps {:dest "lib"}}}
   :profiles/dev  {}
   :profiles/test {}
   :project/dev   {:dependencies [[duct/generate "0.8.2"]
                                  [reloaded.repl "0.2.3"]
                                  [org.clojure/tools.namespace "0.3.1"]
                                  [eftest "0.5.9"]
                                  [com.gearswithingears/shrubbery "0.4.1"]
                                  [kerodon "0.8.0"]
                                  [com.clojure-goes-fast/clj-memory-meter "0.1.2"]]
                   :repl-options   {:init-ns dev
                                    :init (do
                                            (println "Starting BackEnd ...")
                                            (go))
                                    :host    "0.0.0.0"
                                    :port    47480}
                   :source-paths   ["dev/src"]
                   :resource-paths ["dev/resources"]
                   :env {:port "3000"}}
   :project/test  {}})
