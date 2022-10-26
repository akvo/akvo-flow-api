(ns org.akvo.flow-api.boundary.remote-api
  (:require [org.akvo.flow-api.boundary.akvo-flow-server-config :as afsc]
            [org.akvo.flow-api.component.remote-api])
  (:import [com.google.appengine.tools.remoteapi RemoteApiOptions]
           [org.akvo.flow_api.component.remote_api RemoteApi LocalApi OverQuotaApi]
           java.io.IOException))

(defprotocol IRemoteApi
  (options [this instance-id]))

(extend-protocol IRemoteApi

  RemoteApi
  (options [this instance-id]
    (let [afsc (:akvo-flow-server-config this)
          port (afsc/port afsc instance-id)
          host (afsc/host afsc instance-id)
          iam-account (afsc/iam-account afsc instance-id)
          p12-path (afsc/p12-path afsc instance-id)
          remote-path "/remote_api"
          options (-> (RemoteApiOptions.)
                      (.server host port)
                      (.remoteApiPath remote-path))]
      (.useServiceAccountCredential options
                                    iam-account
                                    p12-path)
      options))

  LocalApi
  (options [_this _instance-id]
    (let [options (-> (RemoteApiOptions.)
                      (.server "localhost" 8888))]
      (.useDevelopmentServerCredential options)
      options))

  OverQuotaApi
  (options [_this _instance-id]
    (throw (IOException. "<H1>Over Quota</H1>"))))
