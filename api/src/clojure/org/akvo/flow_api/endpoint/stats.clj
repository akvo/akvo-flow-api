(ns org.akvo.flow-api.endpoint.stats
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec.alpha :as s]
            [compojure.core :refer [GET]]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.datastore.stats :as st]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.endpoint.form-instance :as fi]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [ring.util.response :refer [response]]
            [org.akvo.flow-api.datastore :as ds])
  (:import [com.google.appengine.api.datastore DatastoreServiceFactory]))



(def params-spec (s/keys :req-un [::spec/survey-id
                                  ::spec/form-id
                                  ::spec/question-id]))

(defn endpoint* [{:keys [remote-api]}]
  (GET "/stats" {:keys [email instance-id alias params] :as req}
    (ds/with-remote-api remote-api instance-id
      (let [{:keys [survey-id
                    form-id
                    question-id]} (spec/validate-params params-spec
                                                        (rename-keys params {:survey_id :survey-id
                                                                             :form_id :form-id
                                                                             :question_id :question-id}))
            dss (DatastoreServiceFactory/getDatastoreService)
            user-id (user/id-by-email-or-throw-error remote-api instance-id email)
            survey (survey/by-id remote-api instance-id user-id survey-id)
            form (fi/find-form (:forms survey) form-id)]
        (if (some? form)
          (-> (st/question-counts dss {:formId form-id :questionId question-id})
              (response))
          {:status 404
           :body {"formId" form-id
                  "message" "Form not found"}})))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
