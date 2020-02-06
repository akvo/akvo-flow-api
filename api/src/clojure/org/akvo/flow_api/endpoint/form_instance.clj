(ns org.akvo.flow-api.endpoint.form-instance
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec.alpha :as s]
            [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.form-instance :as form-instance]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.endpoint.utils :as utils]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [ring.util.response :refer [response]])
  (:import java.time.Instant))

(defn find-form [forms form-id]
  (some #(if (= (:id %) form-id)
           %
           nil)
        forms))

(defn next-page-url [api-root instance-id survey-id form-id page-size cursor]
  (utils/url-builder api-root
                     instance-id
                     "form_instances"
                     {"survey_id" survey-id
                      "form_id" form-id
                      "page_size" page-size
                      "cursor" cursor}))

(defn add-next-page-url [form-instances api-root instance-id survey-id form-id page-size]
  (if (empty? (:form-instances form-instances))
    (dissoc form-instances :cursor)
    (-> form-instances
        (assoc :next-page-url
               (next-page-url api-root
                              instance-id
                              survey-id
                              form-id
                              page-size
                              (:cursor form-instances)))
        (dissoc :cursor))))

(defn parse-date
  [^String s]
  (if (.contains s "T")
    (try
      (Instant/parse s)
      (catch Exception _))
    (try
      (Instant/ofEpochSecond (Long/parseLong s))
      (catch Exception _))))

(defn parse-filter
  [^String s]
  (let [[_ op ts] (first (re-seq #"^(>=|>|<=|<)(\S+)$" s))]
    {:operator op
     :timestamp (parse-date ts)}))

(defn valid-filter?
  [^String s]
  (let [parsed (parse-filter s)]
    (boolean (and (:operator parsed) (:timestamp parsed)))))

(s/def ::submission-date (s/nilable valid-filter?))

(def params-spec (s/keys :req-un [::spec/survey-id ::spec/form-id]
                         :opt-un [::spec/cursor ::spec/page-size
                                  ::submission-date]))

(defn endpoint* [{:keys [remote-api]}]
  (GET "/form_instances" {:keys [email instance-id alias params] :as req}
    (ds/with-remote-api remote-api instance-id
      (let [{:keys [survey-id
                    form-id
                    page-size
                    cursor
                    submission-date]} (spec/validate-params params-spec
                                                            (rename-keys params
                                                                         {:survey_id :survey-id
                                                                          :form_id :form-id
                                                                          :page_size :page-size
                                                                          :submission_date :submission-date}))
            page-size (when page-size
                        (Long/parseLong page-size))
            user-id (user/id-by-email-or-throw-error remote-api instance-id email)
            survey (survey/by-id remote-api instance-id user-id survey-id)
            form (find-form (:forms survey) form-id)
            parsed-date (when submission-date
                          (parse-filter submission-date))]
        (if (some? form)
          (-> remote-api
              (form-instance/list instance-id user-id form {:page-size page-size
                                                            :cursor cursor
                                                            :submission-date (:timestamp parsed-date)
                                                            :operator (:operator parsed-date)})
              (add-next-page-url (utils/get-api-root req) alias survey-id form-id page-size)
              (response))
          {:status 404
           :body {"formId" form-id
                  "message" "Form not found"}})))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
