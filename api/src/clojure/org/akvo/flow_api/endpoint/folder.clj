(ns org.akvo.flow-api.endpoint.folder
  (:require [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.folder :as folder]
            [org.akvo.flow-api.boundary.user :as user]
            [ring.util.response :refer [response]])
  (:import [clojure.lang ExceptionInfo]))

(defn add-links [folders api-root instance-id]
  (for [{:keys [id] :as folder} folders]
    (assoc folder
           :surveys-url (format "%s/instance/%s/surveys?folderId=%s" api-root instance-id id)
           :folders-url (format "%s/instance/%s/folders?parentId=%s" api-root instance-id id))))

(defn endpoint [{:keys [remote-api api-root]}]
  (context "/instance" {:keys [email params]}
    (let-routes []
      (GET "/:instance-id/folders" [instance-id]
        (-> remote-api
          (folder/list instance-id
                       (user/id-by-email remote-api instance-id email)
                       (:parentId params))
          (add-links api-root instance-id)
          (response))))))
