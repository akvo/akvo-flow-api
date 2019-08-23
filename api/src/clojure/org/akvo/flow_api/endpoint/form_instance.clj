(ns org.akvo.flow-api.endpoint.form-instance
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec]
            [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.form-instance :as form-instance]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.endpoint.utils :as utils]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [ring.util.response :refer [response]]))

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

(def params-spec (clojure.spec/keys :req-un [::spec/survey-id ::spec/form-id]
                                    :opt-un [::spec/cursor ::spec/page-size]))

(defn endpoint* [{:keys [remote-api api-root]}]
  (GET "/form_instances" {:keys [email instance-id alias params] :as req}
    (let [{:keys [survey-id
                  form-id
                  page-size
                  cursor]} (spec/validate-params params-spec
                                                 (rename-keys params
                                                              {:survey_id :survey-id
                                                               :form_id :form-id
                                                               :page_size :page-size}))
          page-size (when page-size
                      (Long/parseLong page-size))
          user-id (user/id-by-email-or-throw-error remote-api instance-id email)
          survey (survey/by-id remote-api instance-id user-id survey-id)
          form (find-form (:forms survey) form-id)]
      (if (some? form)
        (-> remote-api
            (form-instance/list instance-id user-id form {:page-size page-size
                                                          :cursor cursor})
            (add-next-page-url (utils/get-api-root req) alias survey-id form-id page-size)
            (response))
        {:status 404
         :body {"formId" form-id
                "message" "Form not found"}}))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
