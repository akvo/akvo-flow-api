(defproject stub-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring "1.5.1"]
                 [compojure "1.5.2"]]
  :aot [stub-server.handler]
  :uberjar-name "stub-server.jar"
  :main stub-server.handler)
