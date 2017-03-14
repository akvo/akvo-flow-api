(ns api.core
  (:import [com.google.appengine.tools.remoteapi RemoteApiInstaller RemoteApiOptions]
           [com.gallatinsystems.user.dao UserDao]
           [com.gallatinsystems.survey.dao SurveyDAO]
           [com.gallatinsystems.common Constants]
           [org.apache.commons.codec.binary Base64]
           [com.google.apphosting.utils.config AppEngineWebXmlReader]
           [java.io ByteArrayInputStream])
  (:require [clojure.string :as str]
            [clj-http.client :as http]))

(defn get-filtered-surveys
  [host iam-account p12-path email]
  (let [options (.server (RemoteApiOptions.) host 443)]
    (.useServiceAccountCredential options
                                  iam-account
                                  p12-path)
    (let [installer (RemoteApiInstaller.)]
      (.install installer options)
      (try
        (let [user-dao (UserDao.)
              user (.findUserByEmail user-dao email)
              survey-dao (SurveyDAO.)
              all-surveys (.list survey-dao Constants/ALL_RESULTS)
              user-surveys (.filterByUserAuthorizationObjectId survey-dao
                                                               all-surveys
                                                               (-> user .getKey .getId))]
          (println (.size user-surveys))
          (println (.size all-surveys)))
        (finally
          (.uninstall installer))))))

(defn get-instances [auth-token]
  (let [folders (->> (http/get "https://api.github.com/repos/akvo/akvo-flow-server-config/contents/"
                               {:headers {"Authorization" (format "token %s" auth-token)}
                                :as :json})
                     :body
                     (filter #(= "dir" (:type %)))
                     (map :name))]
    folders))

(defn get-instance-props [instance-id auth-token]
  (let [temp-file (java.io.File/createTempFile "appengine" ".xml")
        _ (spit temp-file
                (String.
                 (Base64/decodeBase64
                  (-> (http/get (format "https://api.github.com/repos/akvo/akvo-flow-server-config/contents/%s/appengine-web.xml"
                                        instance-id)
                                {:headers {"Authorization" (format "token %s"
                                                                   auth-token)}
                                 :as :json})
                      :body
                      :content))))
        ae-reader (AppEngineWebXmlReader. (System/getProperty "java.io.tmpdir")
                                          (.getName temp-file))]
    (.getSystemProperties (.readAppEngineWebXml ae-reader))))

(defn get-instance-map [auth-token]
  (let [instances (get-instances auth-token)]
    (reduce (fn [m {:keys [properties instance-id]}]
              (if (nil? instance-id)
                m
                (assoc m instance-id properties)))
            {}
            (map (fn [instance-id]
                   (try
                     (get-instance-props instance-id auth-token)
                     (catch clojure.lang.ExceptionInfo e)))
                 instances))))
