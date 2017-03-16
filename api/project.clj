(defproject org.akvo/flow-api "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.akvo.flow/data-access "a871631951"]
                 [com.google.appengine/appengine-tools-sdk "1.9.50"]
                 [javax.jdo/jdo2-api "2.3-eb"]
                 [org.datanucleus/datanucleus-core "1.1.5"]
                 [com.google.appengine/appengine-remote-api "1.9.50"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.9.50"]
                 [net.sf.jsr107cache/jsr107cache "1.1"]
                 [servletapi/servlet-api "2.4-20040521"]
                 [com.google.appengine.orm/datanucleus-appengine "1.0.10"]
                 [org.datanucleus/datanucleus-jpa "1.1.5"]
                 [org.apache.geronimo.specs/geronimo-jpa_3.0_spec "1.1.1"]
                 [clj-http "2.3.0"]
                 [cheshire "5.7.0"]]
  :java-source-paths ["java/src"])
