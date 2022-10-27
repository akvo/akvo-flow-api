(ns org.akvo.flow-api.boundary.akvo-flow-server-config
  (:require [org.akvo.flow-api.akvo-flow-server-config :as afsc]
            [org.akvo.flow-api.component.akvo-flow-server-config]))

(defn host [_flow-server-config instance-id]
  (str instance-id ".appspot.com"))

(defn port [_flow-server-config _instance-id]
  443)

(defn iam-account [{:keys [flow-config]} instance-id]
  (let [{:keys [instances]} @flow-config]
    (get-in instances [instance-id "serviceAccountId"])))

(defn p12-path [{:keys [github-auth-token flow-config]} instance-id]
  (.getAbsolutePath (afsc/get-p12-file github-auth-token
                      (-> flow-config deref :tmp-subdir)
                      instance-id)))

(defn asset-url-root [{:keys [flow-config]} instance-id]
  (let [{:keys [instances]} @flow-config]
    (get-in instances [instance-id "photo_url_root"])))
