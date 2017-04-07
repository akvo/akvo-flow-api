(ns org.akvo.flow-api.endpoint.folder
  (:require [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.folder :as folder]
            [org.akvo.flow-api.boundary.user :as user])
  (:import [clojure.lang ExceptionInfo]))

(defn add-links [folders api-root instance-id]
  (for [{:keys [id] :as folder} folders]
    (assoc folder
           :surveys (format "%s/instance/%s/surveys/%s" api-root instance-id id)
           :folders (format "%s/instance/%s/folders/%s" api-root instance-id id))))

(defn endpoint [{:keys [remote-api]}]
  (context "/instance" {:keys [email]}
    (let-routes [api-root "https://api.akvo.org/flow"]
      (GET "/:instance-id/folder/:parent-id" [instance-id parent-id]
        (-> remote-api
          (folder/list instance-id
                       (user/id-by-email remote-api instance-id email)
                       parent-id)
          (add-links api-root instance-id))))))
