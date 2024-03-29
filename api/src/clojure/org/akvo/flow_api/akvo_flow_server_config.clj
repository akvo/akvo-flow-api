(ns org.akvo.flow-api.akvo-flow-server-config
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [com.google.apphosting.utils.config AppEngineWebXmlReader]
           [java.io File]
           [org.apache.commons.codec.binary Base64]))

(def ^:dynamic *github-host* "https://api.github.com")

(defn contents-url [path]
  (format  "%s/repos/akvo/akvo-flow-server-config/contents%s" *github-host* path))

(defn headers [auth-token]
  {"Authorization" (format "token %s" auth-token)
   "Content-Type" "application/json"
   "User-Agent" "flowApi"})

(defn github-contents [auth-token path]
  (:body (http/get (contents-url path)
                   {:headers (headers auth-token)
                    :as :json})))

(defn dir? [content-element]
  (= "dir" (:type content-element)))

(defn fetch-instance-ids [auth-token]
  (->> (github-contents auth-token "/")
       (filter dir?)
       (map :name)))

(defn fetch-file
  "Fetch the file from github. Save it and return the File.
  Returns nil if the file can't be found"
  [auth-token tmp-dir instance-id file-path]
  (let [bytes (-> (github-contents auth-token file-path)
                  ^String (get :content)
                  Base64/decodeBase64)
        file-name (.getName (io/file file-path))
        file (io/file tmp-dir instance-id file-name)]
    (.mkdirs (.getParentFile file))
    (io/copy bytes file)
    file))

(defn get-file ^File [auth-token tmp-dir instance-id file-path]
  (let [file-name (.getName (io/file file-path))
        file (io/file tmp-dir instance-id file-name)]
    (if (.exists file)
      file
      (fetch-file auth-token tmp-dir instance-id file-path))))

(defn get-p12-file ^File [auth-token tmp-dir instance-id]
  (get-file auth-token tmp-dir instance-id (format "/%1$s/%1$s.p12" instance-id)))

(defn get-appengine-web-xml-file ^File [auth-token tmp-dir instance-id]
  (get-file auth-token tmp-dir instance-id (format "/%s/appengine-web.xml" instance-id)))


(defn read-instance-props [path file-name]
  (try
    (let [ae-reader (AppEngineWebXmlReader. path file-name)]
      (.getSystemProperties (.readAppEngineWebXml ae-reader)))
    (catch Exception _)))

(defn get-instance-props [auth-token tmp-dir instance-id]
  (try
    (let [tmp-file (get-appengine-web-xml-file auth-token tmp-dir instance-id)]
      (read-instance-props (format "%s%s/" tmp-dir instance-id)
                           (.getName tmp-file)))
    (catch clojure.lang.ExceptionInfo _)))



(defn get-instance-map [auth-token tmp-dir]
  (let [instance-ids (fetch-instance-ids auth-token)]
    (reduce (fn [m instance-id]
              (if-let [props (get-instance-props auth-token tmp-dir instance-id)]
                (assoc m instance-id props)
                m))
            {}
            instance-ids)))

(defn get-alias-map [instances-map]
  (reduce (fn [m [instance-id props]]
            (let [url (get props "alias")
                  alias (first (str/split url #"\."))]
              (assoc m alias instance-id)))
          {}
          instances-map))
