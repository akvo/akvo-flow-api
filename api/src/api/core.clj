(ns api.core
  (:require [api.akvo-flow-server-config :as config]
            [api.datastore :as ds]
            [api.datastore.folder :as folder]
            [api.datastore.survey :as survey]))

(comment
  (defn remote-api-spec [instance-map instance-id]
    (let [auth-token (System/getenv "GITHUB_API_KEY")
          host (str instance-id ".appspot.com")
          iam-account (get-in instance-map [instance-id "serviceAccountId"])
          p12-file (config/get-p12 instance-id auth-token)]
      {:host host
       :iam-account iam-account
       :p12-path (.getAbsolutePath p12-file)}))

  (let [auth-token (System/getenv "GITHUB_API_KEY")
        email "@gmail.com"
        instance-map (config/get-instance-map auth-token)
        alias-map (config/get-alias-map instance-map)
        instance-id (get alias-map "uat1")
        host (str instance-id ".appspot.com")
        port 443
        iam-account (get-in instance-map [instance-id "serviceAccountId"])
        p12-file (config/get-p12 instance-id auth-token)]
    (folder/get-filtered-folders {:host host
                                  :iam-account iam-account
                                  :p12-path (.getAbsolutePath p12-file)}
                                 email
                                 "27009117"))

  (let [auth-token (System/getenv "GITHUB_API_KEY")
        email "@gmail.com"
        instance-map (config/get-instance-map auth-token)
        alias-map (config/get-alias-map instance-map)
        instance-id (get alias-map "uat1")
        host (str instance-id ".appspot.com")
        port 443
        iam-account (get-in instance-map [instance-id "serviceAccountId"])
        p12-file (config/get-p12 instance-id auth-token)]
    (survey/get-filtered-surveys {:host host
                                  :iam-account iam-account
                                  :p12-path (.getAbsolutePath p12-file)}
                                 email
                                 "24109115"))

  (def auth-token (System/getenv "GITHUB_API_KEY"))
  (def instance-map (config/get-instance-map auth-token))

  (ds/with-remote-api (remote-api-spec instance-map "akvoflow-uat1")
    (survey/get-survey-definition "@gmail.com" "31929121"))

  )
