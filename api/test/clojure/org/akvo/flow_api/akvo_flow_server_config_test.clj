(ns org.akvo.flow-api.akvo-flow-server-config-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [org.akvo.flow-api.component.akvo-flow-server-config :as server-config-component]
            [org.akvo.flow-api.boundary.akvo-flow-server-config :as server-config]
            org.akvo.flow-api.akvo-flow-server-config
            [org.akvo.flow-api.boundary.resolve-alias :as resolve-alias]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [clojure.string :as s])
  (:import (org.apache.commons.codec.binary Base64)))

(def wiremock-url "http://wiremock-proxy:8080")
(def wiremock-mappings-url (str wiremock-url "/__admin/mappings"))

(defn setup-folders [folders]
  (http/post wiremock-mappings-url
             {:body (json/generate-string {"request"  {"method"  "GET"
                                                       "urlPath" "/repos/akvo/akvo-flow-server-config/contents/"}
                                           "response" {"status"   200
                                                       "jsonBody" (cons {"name" ".gitignore"
                                                                         "sha"  "c4f9d499061fcc1db85c60dd432689ab7f6793d1"
                                                                         "type" "file"}
                                                                        (map
                                                                          (fn [folder]
                                                                            {"name" folder
                                                                             "sha"  "8508e1be2b5623cabbd29068dcadde46a7794e20"
                                                                             "type" "dir"})
                                                                          folders))}})}))

(defn setup-appengine-xml [{:keys [name appengine-alias]}]
  (http/post wiremock-mappings-url
             {:body (json/generate-string {"request"  {"method"  "GET"
                                                       "urlPath" (str "/repos/akvo/akvo-flow-server-config/contents/" name "/appengine-web.xml")}
                                           "response" {"status"   200
                                                       "jsonBody" {"name"    "appengine-web.xml"
                                                                   "sha"     "some sha"
                                                                   "content" (Base64/encodeBase64String (.getBytes
                                                                                                          (s/replace (slurp (io/resource "org/akvo/flow_api/appengine-web.xml"))
                                                                                                                     "aliastobechanged"
                                                                                                                     appengine-alias)
                                                                                                          "UTF-8"))
                                                                   "type"    "file"}}})}))

(defn setup-p12-file [{:keys [^String name ^String p12-content]}]
  (http/post wiremock-mappings-url
             {:body (json/generate-string {"request"  {"method"  "GET"
                                                       "urlPath" (str "/repos/akvo/akvo-flow-server-config/contents/" name "/" name ".p12")}
                                           "response" {"status"   200
                                                       "jsonBody" {"name"    "appengine-web.xml"
                                                                   "sha"     "some sha"
                                                                   "content" (Base64/encodeBase64String (.getBytes p12-content "UTF-8"))
                                                                   "type"    "file"}}})}))

(defn reset-wiremock []
  (http/post (str wiremock-mappings-url "/reset")))

(defn setup-github [instances]
  (reset-wiremock)
  (setup-folders (map :name instances))
  (doseq [instance instances]
    (setup-appengine-xml instance)
    (setup-p12-file instance)))

(deftest github-integration
  (let [instance-name (str "akvoflow-instance-" (System/currentTimeMillis))
        instance-alias (str instance-name "-alias")
        p12-content (str instance-name "-p12-content")
        instances [{:name            instance-name
                    :appengine-alias instance-alias
                    :p12-content     p12-content}]]
    (setup-github instances)
    (binding [org.akvo.flow-api.akvo-flow-server-config/*github-host* wiremock-url]
      (let [server-conf (component/start (server-config-component/akvo-flow-server-config {:github-auth-token "any token" :tmp-dir "/tmp/"}))]
        (testing "initial load"
          (is (= p12-content (slurp (server-config/p12-path server-conf instance-name))))
          (is (= "this is a hack to force the remote API to use localhost" (server-config/iam-account server-conf instance-name)))
          (is (= "http://s3/images/" (server-config/asset-url-root server-conf instance-name)))
          (is (= instance-name (resolve-alias/resolve server-conf instance-name)))
          (is (= instance-name (resolve-alias/resolve server-conf instance-alias))))

        (testing "refresh appengine xml"
          (setup-github [{:name            instance-name
                          :appengine-alias "updated-alias"
                          :p12-content     p12-content}])
          (server-config-component/refresh! server-conf)
          (is (= instance-name (resolve-alias/resolve server-conf "updated-alias")))
          (is (nil? (resolve-alias/resolve server-conf instance-alias))))

        (testing "refresh p12 file"
          (setup-github [{:name            instance-name
                          :appengine-alias instance-alias
                          :p12-content     "new p12 content"}])
          (server-config-component/refresh! server-conf)
          (is (= "new p12 content" (slurp (server-config/p12-path server-conf instance-name)))))

        (testing "new instance"
          (setup-github [{:name            instance-name
                          :appengine-alias instance-alias
                          :p12-content     p12-content}
                         {:name            "new-instance"
                          :appengine-alias "new-instance-alias"
                          :p12-content     "new p12 content"}])
          (server-config-component/refresh! server-conf)
          (is (= "new p12 content" (slurp (server-config/p12-path server-conf "new-instance"))))
          (is (= "new-instance" (resolve-alias/resolve server-conf "new-instance")))
          (is (= "new-instance" (resolve-alias/resolve server-conf "new-instance-alias"))))))))
