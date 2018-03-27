(defproject org.akvo/flow-api "0.1.0-SNAPSHOT"
  :description "Akvo Flow API"
  :url "https://github.com/akvo/akvo-flow-api"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]
                 [org.clojure/core.cache "0.6.5"]
                 [com.stuartsierra/component "0.3.1"]
                 [compojure "1.5.1"]
                 [duct "0.8.2"]
                 [environ "1.1.0"]
                 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-jetty-component "0.3.1"]
                 [org.akvo.flow/data-access "v1.9.29"]
                 [org.akvo/commons "0.4.5"]
                 [raven-clj "1.5.0"]
                 [javax.jdo/jdo2-api "2.3-eb"]
                 [com.google.appengine/appengine-tools-sdk "1.9.50"]
                 [com.google.appengine/appengine-remote-api "1.9.50"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.9.50"]
                 [com.google.appengine/appengine-jsr107cache "1.9.50"]
                 [net.sf.jsr107cache/jsr107cache "1.1"]
                 [javax.servlet/servlet-api "2.5"]
                 [org.apache.geronimo.specs/geronimo-jpa_3.0_spec "1.1.1"]
                 [clj-http "2.3.0"]
                 [cheshire "5.7.0"]]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clojure"]
  :plugins [[lein-environ "1.0.3"]]
  :main ^:skip-aot org.akvo.flow-api.main
  :target-path "target/%s/"
  :aliases {"setup"  ["run" "-m" "duct.util.repl/setup"]}
  :uberjar-name "akvo-flow-api.jar"
  :profiles
  {:dev  [:project/dev  :profiles/dev]
   :test [:project/test :profiles/test]
   :uberjar {:aot :all}
   :profiles/dev  {}
   :profiles/test {}
   :project/dev   {:dependencies [[org.datanucleus/datanucleus-jpa "1.1.5"]
                                  [org.datanucleus/datanucleus-core "1.1.5"]
                                  [com.google.appengine.orm/datanucleus-appengine "1.0.10"]
                                  [duct/generate "0.8.2"]
                                  [reloaded.repl "0.2.3"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [eftest "0.1.1"]
                                  [com.gearswithingears/shrubbery "0.4.1"]
                                  [kerodon "0.8.0"]]
                   :source-paths   ["dev/src"]
                   :resource-paths ["dev/resources"]
                   :repl-options {:init-ns user}
                   :env {:port "3000"}}
   :project/test  {}})
