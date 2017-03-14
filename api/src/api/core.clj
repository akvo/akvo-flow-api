(ns api.core
  (:import [com.google.appengine.tools.remoteapi RemoteApiInstaller RemoteApiOptions]
           [com.gallatinsystems.user.dao UserDao]
           [com.gallatinsystems.survey.dao SurveyDAO]
           [com.gallatinsystems.common Constants]))

(defn get-filtered-surveys
  [host iam-account p12-path email]
  (let [options (.server (RemoteApiOptions.) host 443)]
    (.useServiceAccountCredential options
                                  iam-account
                                  p12-path)
    (let [installer (RemoteApiInstaller.)]
      (.install installer options)
      (try
        (let [user-dao (UserDao.)
              user (.findUserByEmail user-dao email)
              survey-dao (SurveyDAO.)
              all-surveys (.list survey-dao Constants/ALL_RESULTS)
              user-surveys (.filterByUserAuthorizationObjectId survey-dao
                                                               all-surveys
                                                               (-> user .getKey .getId))]
          (println (.size user-surveys))
          (println (.size all-surveys)))
        (finally
          (.uninstall installer))))))
