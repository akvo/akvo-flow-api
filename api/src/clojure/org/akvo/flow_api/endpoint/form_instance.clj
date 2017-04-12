(ns org.akvo.flow-api.endpoint.form-instance
  (:require [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.form-instance :as form-instance]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [ring.util.response :refer [response]]))

(defn find-form [forms form-id]
  (some #(if (= (:id %) form-id)
           %
           nil)
        forms))

(defn cursor-url-fn [api-root instance-id survey-id form-id page-size]
  (fn [cursor]
    (format "%s/orgs/%s/form-instances/%s/%s?%scursor=%s"
            api-root
            instance-id
            survey-id
            form-id
            (if page-size
              (format "pageSize=%s&" page-size)
              "")
            cursor)))

(defn add-cursor [form-instances api-root instance-id survey-id form-id page-size]
  (if (empty? (:form-instances form-instances))
    (dissoc form-instances :cursor)
    (update form-instances :cursor (cursor-url-fn api-root
                                                  instance-id
                                                  survey-id
                                                  form-id
                                                  page-size))))

(defn endpoint* [{:keys [remote-api api-root]}]
  (GET "/form-instances/:survey-id/:form-id" {:keys [email instance-id alias params]}
    (let [{page-size :pageSize cursor :cursor survey-id :survey-id form-id :form-id} params
          page-size (when page-size
                      (Long/parseLong page-size))
          user-id (user/id-by-email remote-api instance-id email)
          survey (survey/by-id remote-api instance-id user-id survey-id)
          form (find-form (:forms survey) form-id)]
      (-> remote-api
          (form-instance/list instance-id user-id form {:page-size page-size
                                                        :cursor cursor})
          (add-cursor api-root instance-id survey-id form-id page-size)
          (response)))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)))
