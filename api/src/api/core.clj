(ns api.core
  (:import [com.google.appengine.tools.remoteapi RemoteApiInstaller RemoteApiOptions]
           [com.gallatinsystems.user.dao UserDao]
           [com.gallatinsystems.survey.dao SurveyDAO]
           [com.gallatinsystems.common Constants]
           [org.apache.commons.codec.binary Base64]
           [com.google.apphosting.utils.config AppEngineWebXmlReader]
           [java.io ByteArrayInputStream])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-http.client :as http]))

(def p12-dir (System/getProperty "java.io.tmpdir"))

#_(let [auth-token ""
        email ""
        alias-map (get-alias-map instance-map)
        instance-id (get alias-map "uat1")
        host (str instance-id ".appspot.com")
        port 443
        iam-account (get-in instance-map [instance-id "serviceAccountId"])
        p12-file (get-p12 instance-id auth-token)]
    (get-filtered-surveys host iam-account (.getAbsolutePath p12-file) email))

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
    {:instance-id instance-id
     :properties (.getSystemProperties (.readAppEngineWebXml ae-reader))}))

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

(defn get-alias-map [instances-map]
  (reduce (fn [m [instance-id props]]
            (let [url (get props "alias")
                  alias (first (str/split url #"\."))]
              (assoc m alias instance-id)))
          {}
          instances-map))



(defn fetch-p12
  "Fetch the p12 file from github. Save it and return the File.
  Returns nil if the file can't be found"
  [instance-id auth-token]
  (try
    (let [bytes (-> (http/get (format
                               "https://api.github.com/repos/akvo/akvo-flow-server-config/contents/%1$s/%1$s.p12"
                               instance-id)
                              {:headers {"Authorization" (str "token " auth-token)
                                         "Content-Type" "application/json"}
                               :as :json})
                    :body
                    :content
                    Base64/decodeBase64)
          file (io/file p12-dir (str instance-id ".p12"))]
      (io/copy bytes file)
      file)
    (catch clojure.lang.ExceptionInfo e)))

(defn get-p12 [instance-id auth-token]
  (let [file (io/file p12-dir (str instance-id ".p12"))]
    (if (.exists file)
      file
      (fetch-p12 instance-id auth-token))))
