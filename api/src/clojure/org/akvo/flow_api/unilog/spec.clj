(ns org.akvo.flow-api.unilog.spec
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))
;; This is a copy of the Authz spec. Completely useless but for removing Authz events and how we did a per event type spec.

(def id-seq (atom 1))
(s/def ::id (s/with-gen integer? #(gen/fmap (fn [_] (swap! id-seq inc)) (gen/return nil))))

(s/def ::name string?)

(s/def ::language string?)
(s/def ::userName string?)
(s/def ::emailAddress string?)
(s/def ::permissionList #{"20" "0" "10"})
(s/def ::superAdmin boolean?)

(s/def ::user
  (s/keys
    :req-un
    [::emailAddress ::id ::permissionList ::superAdmin]
    :opt-un
    [::language ::userName]))

(s/def ::roleId ::id)
(s/def ::userId ::id)
(s/def ::securedObjectId ::id)

(s/def ::userAuthorization
  (s/keys :req-un [::id ::roleId ::securedObjectId ::userId]))

(s/def ::permissions (s/coll-of #{"DATA_UPDATE" "FORM_READ" "PROJECT_FOLDER_CREATE" "DATA_DELETE"
                                  "DATA_CLEANING" "FORM_UPDATE" "CASCADE_MANAGE"
                                  "PROJECT_FOLDER_READ" "DATA_READ" "FORM_CREATE" "DEVICE_MANAGE"
                                  "DATA_APPROVE_MANAGE" "PROJECT_FOLDER_DELETE" "FORM_DELETE"
                                  "PROJECT_FOLDER_UPDATE"}))

(s/def ::userRole
  (s/keys :req-un [::id ::name]
    :opt-un [::permissions]))

(s/def ::parentId ::id)
(s/def ::public boolean?)
(s/def ::surveyGroupType #{"FOLDER" "SURVEY"})
(s/def ::description string?)

(s/def ::surveyGroup
  (s/keys
    :req-un
    [::id ::name ::public ::surveyGroupType ::parentId]
    :opt-un
    [::description]))

(s/def ::delete
  (s/keys :req-un [::id]))

(s/def ::orgId string?)

(defn types-for [kind]
  #{(str kind "Updated")
    (str kind "Created")
    (str kind "Deleted")})

(s/def ::context (s/keys :opt-un [::timestamp]))
(s/def ::timestamp
  (s/with-gen nat-int? #(gen/fmap
                          (fn [x]
                            (if (zero? x)
                              x
                              (- (System/currentTimeMillis) (* 1000 x))))
                          (s/gen int?))))

(s/def ::eventType (set (mapcat types-for ["surveyGroup" "user" "userAuthorization" "userRole"])))
(s/def ::entity map?)
(s/def ::payload (s/keys :req-un [::eventType ::entity ::orgId] :opt-un [::context]))
(s/def ::event (s/keys :req-un [::id ::payload]))

(defn valid? [m]
  (and
    (s/valid? ::event m)
    (s/valid?
      (case (-> m :payload :eventType)
        ("surveyGroupDeleted" "userDeleted" "userAuthorizationDeleted" "userRoleDeleted") ::delete
        ("surveyGroupUpdated" "surveyGroupCreated") ::surveyGroup
        ("userCreated" "userUpdated") ::user
        ("userAuthorizationCreated" "userAuthorizationUpdated") ::userAuthorization
        ("userRoleCreated" "userRoleUpdated") ::userRole)
      (-> m :payload :entity))))

(defn explain [m]
  (s/explain ::event m)
  (s/explain
    (case (-> m :payload :eventType)
      ("surveyGroupDeleted" "userDeleted" "userAuthorizationDeleted" "userRoleDeleted") ::delete
      ("surveyGroupUpdated" "surveyGroupCreated") ::surveyGroup
      ("userCreated" "userUpdated") ::user
      ("userAuthorizationCreated" "userAuthorizationUpdated") ::userAuthorization
      ("userRoleCreated" "userRoleUpdated") ::userRole)
    (-> m :payload :entity)))

(comment
  (gen/sample (s/gen ::surveyGroup))

  (valid? {:id 1
           :payload {:orgId "h"
                     :eventType "userCreated"
                     :context {:timestamp 0}
                     :entity {:id 12
                              :language "x"
                              :permissionList "10"
                              :superAdmin true
                              :emailAddress "vrgc7P6dbwB57F"}}}))