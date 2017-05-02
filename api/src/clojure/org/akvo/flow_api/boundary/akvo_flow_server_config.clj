(ns org.akvo.flow-api.boundary.akvo-flow-server-config
  (:require [org.akvo.flow-api.akvo-flow-server-config :as afsc]
            [org.akvo.flow-api.component.akvo-flow-server-config])
  (:import [org.akvo.flow_api.component.akvo_flow_server_config AkvoFlowServerConfig]))

(defprotocol IAkvoFlowServerConfig
  (host [this instance-id])
  (port [this instance-id])
  (iam-account [this instance-id])
  (p12-path [this instance-id])
  (trace-path [this instance-id])
  (asset-url-root [this instance-id]))

(extend-protocol IAkvoFlowServerConfig
  AkvoFlowServerConfig
  (host [this instance-id]
    (str instance-id ".appspot.com"))
  (port [this instance-id]
    443)
  (iam-account [{:keys [instances]} instance-id]
    (get-in instances [instance-id "serviceAccountId"]))
  (p12-path [{:keys [github-auth-token tmp-dir]} instance-id]
    (.getAbsolutePath (afsc/get-p12-file github-auth-token
                                         tmp-dir
                                         instance-id)))
  (trace-path [this instance-id]
    nil)
  (asset-url-root [{:keys [instances]} instance-id]
    (get-in instances [instance-id "photo_url_root"])))
