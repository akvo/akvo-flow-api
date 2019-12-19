(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [com.stuartsierra.component :as component]
            [duct.generate :as gen]
            [eftest.runner :as eftest]
            [duct.util.repl :refer [setup cljs-repl migrate rollback]]
            [duct.util.system :refer [load-system]]
            [reloaded.repl :refer [system init start stop go reset]]))

(defn new-system []
  (load-system (keep io/resource ["org/akvo/flow_api/system.edn" "dev.edn" "local.edn"])))

(defn test []
  (eftest/run-tests (->> (eftest/find-tests "test")
                         (remove (fn [t] (or (-> t meta :kubernetes-test)
                                             (-> t meta :ns meta :kubernetes-test)))))))

(when (io/resource "local.clj")
  (load "local"))

(gen/set-ns-prefix 'org.akvo.flow-api)

(reloaded.repl/set-init! new-system)

(comment
  (->
   (clj-http.client/get "http://localhost:3000/orgs/akvoflowsandbox/surveys"
                        {:as :json
                         :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                         :content-type :json})
   :body
   )


  (-> (clj-http.client/get "http://localhost:3000/orgs/akvoflowsandbox/folders"
                        {:as :json
                         :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
                         
                         :content-type :json}
                        )
      :body)

    



{:surveys
 [{:id "152342023",
   :name "BAR-handpump",
   :folderId "153142013",
   :createdAt "2017-03-27T08:47:23.830Z",
   :modifiedAt "2017-03-27T08:53:53.184Z",
   :surveyUrl
   "https://localhost:3000/flow/orgs/akvoflowsandbox/surveys/152342023"}
  {:id "148412306",
   :name "NR-handpump",
   :folderId "153142013",
   :createdAt "2017-03-27T09:05:51.743Z",
   :modifiedAt "2017-03-27T09:07:27.506Z",
   :surveyUrl
   "https://localhost:3000/flow/orgs/akvoflowsandbox/surveys/148412306"}]}  
  )
