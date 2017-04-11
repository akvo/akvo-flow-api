(ns org.akvo.flow-api.endpoint.folder
  (:require [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.folder :as folder]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.middleware.anomaly :refer [wrap-anomaly]]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [ring.util.response :refer [response]]))

(defn add-links [folders api-root instance-id]
  (for [{:keys [id] :as folder} folders]
    (assoc folder
           :surveys-url (format "%s/orgs/%s/surveys?folderId=%s" api-root instance-id id)
           :folders-url (format "%s/orgs/%s/folders?parentId=%s" api-root instance-id id))))

(defn endpoint* [{:keys [remote-api akvo-flow-server-config api-root]}]
  (GET "/folders" {:keys [email instance-id alias params]}
    (-> remote-api
      (folder/list instance-id
                   (user/id-by-email remote-api instance-id email)
                   (:parentId params))
      (add-links api-root alias)
      (response))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)
      (wrap-anomaly)))
