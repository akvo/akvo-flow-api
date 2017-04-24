(ns org.akvo.flow-api.endpoint.folder
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec]
            [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.folder :as folder]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [ring.util.response :refer [response]]))

(defn add-links [folders api-root instance-id]
  (for [{:keys [id] :as folder} folders]
    (assoc folder
           :surveys-url (format "%sorgs/%s/surveys?folderId=%s" api-root instance-id id)
           :folders-url (format "%sorgs/%s/folders?parentId=%s" api-root instance-id id))))

(def params-spec (clojure.spec/keys :opt-un [::spec/parent-id]))

(defn endpoint* [{:keys [remote-api akvo-flow-server-config api-root]}]
  (GET "/folders" {:keys [email instance-id alias params]}
    (let [{:keys [parent-id]} (spec/validate-params params-spec
                                                    (rename-keys params
                                                                 {:parentId :parent-id}))]
      (-> remote-api
          (folder/list instance-id
                       (user/id-by-email remote-api instance-id email)
                       (or parent-id "0"))
          (add-links api-root alias)
          (response)))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)))
