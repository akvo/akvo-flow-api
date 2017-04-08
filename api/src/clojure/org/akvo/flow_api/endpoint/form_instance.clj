(ns org.akvo.flow-api.endpoint.form-instance
  (:require [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.form-instance :as form-instance]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.endpoint.anomaly :as anomaly]
            [ring.util.response :refer [response]])
  (:import [clojure.lang ExceptionInfo]))

(defn find-form [forms form-id]
  (some #(if (= (:id %) form-id)
           %
           nil)
        forms))

(defn add-cursor [form-instances api-root instance-id survey-id form-id page-size]
  (if (empty? (:form-instances form-instances))
    (dissoc form-instances :cursor)
    (update form-instances :cursor #(format "%s/instance/%s/form-instances/%s/%s?%scursor=%s"
                                            api-root
                                            instance-id
                                            survey-id
                                            form-id
                                            (if page-size
                                              (format "pageSize=%s&" page-size)
                                              "")
                                            %))))

(defn endpoint [{:keys [remote-api api-root]}]
  (context "/instance" {:keys [email params]}
    (let-routes []
      (GET "/:instance-id/form-instances/:survey-id/:form-id" [instance-id survey-id form-id]
        (try
          (let [{page-size :pageSize cursor :cursor} params
                page-size (when page-size
                            (Long/parseLong page-size))
                user-id (user/id-by-email remote-api instance-id email)
                survey (survey/by-id remote-api instance-id user-id survey-id)
                form (find-form (:forms survey) form-id)]
            (-> remote-api
                (form-instance/list instance-id user-id form {:page-size page-size
                                                              :cursor cursor})
                (add-cursor api-root instance-id survey-id form-id page-size)
                (response)))
          (catch ExceptionInfo e
            (anomaly/handle e)))))))
