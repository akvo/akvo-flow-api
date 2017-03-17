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

(defn get-instances [auth-token]
  (->> (github-contents auth-token "/")
       (filter #(= "dir" (:type %)))
       (map :name)))

(defn get-instance-props [instance-id auth-token]
  (let [tmp-file (java.io.File/createTempFile "appengine" ".xml")
        _ (spit tmp-file
                (-> (github-contents auth-token (format "/%s/appengine-web.xml" instance-id))
                    :content
                    Base64/decodeBase64
                    String.))
        ae-reader (AppEngineWebXmlReader. tmp-dir
                                          (.getName tmp-file))]
    (.getSystemProperties (.readAppEngineWebXml ae-reader))))

(defn get-instance-map [auth-token]
  (let [instances (get-instances auth-token)]
    (reduce (fn [m instance-id]
              (try
                (assoc m instance-id (get-instance-props instance-id auth-token))
                (catch Exception e m)))
            {}
            ["akvoflow-uat1"];; instances
            )))

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
    (let [bytes (-> (github-contents auth-token (format "/%1$s/%1$s.p12" instance-id))
                    :content
                    Base64/decodeBase64)
          file (io/file tmp-dir (str instance-id ".p12"))]
      (io/copy bytes file)
      file)
    (catch clojure.lang.ExceptionInfo e)))

(defn get-p12 [instance-id auth-token]
  (let [file (io/file tmp-dir (str instance-id ".p12"))]
    (if (.exists file)
      file
      (fetch-p12 instance-id auth-token))))
