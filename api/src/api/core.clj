(ns api.core
  (:import [com.gallatinsystems.common Constants]
           [com.gallatinsystems.survey.dao.SurveyDAO]
           [com.gallatinsystems.user.dao UserDao]
           [com.google.appengine.tools.remoteapi RemoteApiInstaller RemoteApiOptions]
           [com.google.apphosting.utils.config AppEngineWebXmlReader]
           [java.io ByteArrayInputStream]
           [java.time.format DateTimeFormatter]
           [org.akvo.flow.api.dao FolderDAO SurveyDAO]
           [org.apache.commons.codec.binary Base64])
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def tmp-dir (System/getProperty "java.io.tmpdir"))

(defmacro with-remote-api [spec & body]
  `(let [host# (get ~spec :host)
         port# (get ~spec :port 443)
         iam-account# (get ~spec :iam-account)
         p12-path# (get ~spec :p12-path)
         remote-path# (let [trace-path# (get ~spec :trace-path)]
                        (if (nil? trace-path#)
                          "/remote_api"
                          (str "/traced_remote_api/" trace-path#)))
         options# (-> (RemoteApiOptions.)
                      (.server host# port#)
                      (.remoteApiPath remote-path#))]
     (.useServiceAccountCredential options#
                                   iam-account#
                                   p12-path#)
     (let [installer# (RemoteApiInstaller.)]
       (.install installer# options#)
       (try
         ~@body
         (finally
           (.uninstall installer#))))))

(def date-format (.toFormat (DateTimeFormatter/ISO_INSTANT)))

(defn to-iso-8601 [date]
  (.format date-format (.toInstant date)))

(defn created-at [entity]
  (to-iso-8601 (.getCreatedDateTime entity)))

(defn modified-at [entity]
  (to-iso-8601 (.getLastUpdateDateTime entity)))

(defn id [entity]
  (-> entity .getKey .getId))


(defn get-filtered-folders [spec email parent-id]
  (with-remote-api spec
    (let [user-dao (UserDao.)
          user (.findUserByEmail user-dao email)
          folder-dao (FolderDAO.)
          all-folders (.listAll folder-dao)
          user-folders (.filterByUserAuthorizationObjectId folder-dao
                                                           all-folders
                                                           (-> user .getKey .getId))]

      (->> user-folders
           (map (fn [folder]
                  {:id (str (id folder))
                   :name (.getName folder)
                   :parent-id (str (.getParentId folder))
                   :created-at (created-at folder)
                   :modified-at (modified-at folder)}))
           (filter #(= (:parent-id %) parent-id))))))

(defn get-filtered-surveys [spec email folder-id]
  (with-remote-api spec
    (let [user-dao (UserDao.)
          user (.findUserByEmail user-dao email)
          survey-dao (SurveyDAO.)
          all-surveys (.listAll survey-dao)
          user-surveys (.filterByUserAuthorizationObjectId survey-dao
                                                           all-surveys
                                                           (-> user .getKey .getId))]

      (->> user-surveys
           (map (fn [survey]
                  {:id (str (id survey))
                   :name (.getName survey)
                   :folder-id (str (.getParentId survey))
                   :created-at (created-at survey)
                   :modified-at (modified-at survey)}))
           (filter #(= (:folder-id %) folder-id))))))

(defn ->question [question]
  {:id (str (id question))
   :name (.getText question)
   :type (str (.getType question))
   :order (.getOrder question)
   :created-at (created-at question)
   :modified-at (modified-at question)})



(defn question-group-definition [question-group questions]
  (let [qs (sort-by :order (map ->question questions))]
    {:id (str (id question-group))
     :name (.getName question-group)
     :repeatable? (.getRepeatable question-group)
     :questions qs
     :created-at (created-at question-group)
     :modified-at (modified-at question-group)}))

(defn get-form-definition [email form-id]
  (let [form-dao (com.gallatinsystems.survey.dao.SurveyDAO.)
        ;; Includes question groups, but contrary to docstring does not contain questions
        form (.loadFullSurvey form-dao form-id)
        question-dao (com.gallatinsystems.survey.dao.QuestionDao.)
        questions (group-by #(.getQuestionGroupId %)
                            (.listQuestionsBySurvey question-dao form-id))]
    {:id (str (id form))
     :name (.getName form)
     :question-groups (mapv (fn [question-group]
                              (question-group-definition question-group
                                                         (get questions
                                                              (id question-group))))
                            (.values (.getQuestionGroupMap form)))
     :created-at (created-at form)
     :modified-at (modified-at form)}))

(defn get-survey-definition [email survey-id]
  (let [user-dao (UserDao.)
        user (.findUserByEmail user-dao email)
        survey-dao (com.gallatinsystems.survey.dao.SurveyGroupDAO.)
        survey (.getByKey survey-dao (Long/parseLong survey-id))
        form-dao (com.gallatinsystems.survey.dao.SurveyDAO.)
        forms (.filterByUserAuthorizationObjectId form-dao
                                                  (.listSurveysByGroup form-dao (Long/parseLong survey-id))
                                                  (id user))]
    {:id survey-id
     :name (.getName survey)
     :forms (mapv #(get-form-definition email (id %))
                  forms)
     :created-at (created-at survey)
     :modified-at (modified-at survey)}))

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

(comment
  (defn remote-api-spec [instance-map instance-id]
    (let [auth-token (System/getenv "GITHUB_API_KEY")
          host (str instance-id ".appspot.com")
          iam-account (get-in instance-map [instance-id "serviceAccountId"])
          p12-file (get-p12 instance-id auth-token)]
      {:host host
       :iam-account iam-account
       :p12-path (.getAbsolutePath p12-file)}))

  (let [auth-token (System/getenv "GITHUB_API_KEY")
        email ""
        instance-map (get-instance-map auth-token)
        alias-map (get-alias-map instance-map)
        instance-id (get alias-map "uat1")
        host (str instance-id ".appspot.com")
        port 443
        iam-account (get-in instance-map [instance-id "serviceAccountId"])
        p12-file (get-p12 instance-id auth-token)]
    (get-filtered-folders {:host host
                           :iam-account iam-account
                           :p12-path (.getAbsolutePath p12-file)}
                          email
                          "27009117"))

  (let [auth-token (System/getenv "GITHUB_API_KEY")
        email ""
        instance-map (get-instance-map auth-token)
        alias-map (get-alias-map instance-map)
        instance-id (get alias-map "uat1")
        host (str instance-id ".appspot.com")
        port 443
        iam-account (get-in instance-map [instance-id "serviceAccountId"])
        p12-file (get-p12 instance-id auth-token)]
    (get-filtered-surveys {:host host
                           :iam-account iam-account
                           :p12-path (.getAbsolutePath p12-file)}
                          email
                          "24109115"))

  (def auth-token (System/getenv "GITHUB_API_KEY"))
  (def instance-map (get-instance-map auth-token))

  (remote-api-spec instance-map "akvoflow-uat1")
  (with-remote-api (remote-api-spec instance-map "akvoflow-uat1")
    (get-survey-definition "" "31929121"))

  )
