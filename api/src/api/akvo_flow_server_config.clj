(ns api.akvo-flow-server-config
  (:import [com.google.apphosting.utils.config AppEngineWebXmlReader]
           [java.io ByteArrayInputStream]
           [org.apache.commons.codec.binary Base64])
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def tmp-dir (System/getProperty "java.io.tmpdir"))

(defn contents-url [path]
  (format "https://api.github.com/repos/akvo/akvo-flow-server-config/contents%s" path))

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
  [auth-token instance-id file-path]
  (try
    (let [bytes (-> (github-contents auth-token file-path)
                    :content
                    Base64/decodeBase64)
          file-name (.getName (io/file file-path))
          file (io/file tmp-dir instance-id file-name)]
      (.mkdirs (.getParentFile file))
      (io/copy bytes file)
      file)
    (catch clojure.lang.ExceptionInfo e)))

(defn get-file [auth-token instance-id file-path]
  (let [file-name (.getName (io/file file-path))
        file (io/file tmp-dir instance-id file-name)]
    (if (.exists file)
      file
      (fetch-file auth-token instance-id file-path))))

(defn get-p12-file [auth-token instance-id]
  (get-file auth-token instance-id (format "/%1$s/%1$s.p12" instance-id)))

(defn get-appengine-web-xml-file [auth-token instance-id]
  (get-file auth-token instance-id (format "/%s/appengine-web.xml" instance-id)))

(defn get-instance-props [auth-token instance-id]
  (let [tmp-file (get-appengine-web-xml-file auth-token instance-id)
        ae-reader (AppEngineWebXmlReader. tmp-dir
                                          (.getName tmp-file))]
    (.getSystemProperties (.readAppEngineWebXml ae-reader))))

(defn get-instance-map [auth-token]
  (let [instance-ids (fetch-instance-ids auth-token)]
    (reduce (fn [m instance-id]
              (try
                (assoc m instance-id (get-instance-props auth-token instance-id))
                (catch Exception e m)))
            {}
            instance-ids)))

(defn get-alias-map [instances-map]
  (reduce (fn [m [instance-id props]]
            (let [url (get props "alias")
                  alias (first (str/split url #"\."))]
              (assoc m alias instance-id)))
          {}
          instances-map))
