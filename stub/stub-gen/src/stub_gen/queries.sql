-- :name get-folders
SELECT payload->'key'->>'id' AS id,
       payload->>'properties' as props
       --payload->'properties'->>'code' AS code,
       --payload->'properties'->>'name' AS "name",
       --payload->'properties'->>'parentId' AS parent_id
  FROM survey
 WHERE payload->'properties'->>'projectType' = 'PROJECT_FOLDER'
   AND payload->'properties'->>'parentId' = :parent-id;

-- :name get-surveys
SELECT payload->'key'->>'id' AS id,
       payload->>'properties' as props
       --payload->'properties'->>'code' AS code,
       --payload->'properties'->>'name' AS "name",
       --payload->'properties'->>'parentId' AS parent_id,
       --payload->'properties'->>'monitoringGroup' AS is_monitoring,
       --payload->'properties'->>'newLocaleSurveyId' AS registration_form_id
  FROM survey
 WHERE payload->'properties'->>'projectType' = 'PROJECT'
   AND payload->'properties'->>'parentId' = :folder-id;


-- :name get-forms
SELECT payload->'key'->>'id' AS id,
       payload->>'properties' as props
       --payload->'properties'->>'code' AS code,
       --payload->'properties'->>'name' AS "name",
       --payload->'properties'->>'surveyGroupId' AS survey_id,
       --payload->'properties'->>'status' AS status,
       --payload->'properties'->>'defaultLanguageCode' AS default_lang
  FROM form
 WHERE payload->'properties'->>'surveyGroupId' = :survey-id;

-- :name get-question-groups
SELECT payload->'key'->>'id' AS id,
       payload->>'properties' as props
       --payload->'properties'->>'code' AS code,
       --payload->'properties'->>'name' AS "name",
       --payload->'properties'->>'surveyId' AS form_id,
       --(payload->'properties'->>'order')::numeric as "order",
       --payload->'properties'->>'defaultLanguageCode' AS default_lang
  FROM question_group
 WHERE payload->'properties'->>'surveyId' = :form-id;

-- :name get-questions
SELECT payload->'key'->>'id' AS id,
       payload->>'properties' as props
  FROM question
 WHERE payload->'properties'->>'questionGroupId' = :question-group-id;


-- :name get-question-options
SELECT payload->'key'->>'id' as id,
       payload->>'properties' as props
  FROM question_option a,
       (
       SELECT payload->'key'->>'id' AS question_id
         FROM question
        WHERE payload->'properties'->>'surveyId' = :form-id
       ) b
 WHERE a.payload->'properties'->>'questionId' = b.question_id;

-- :name get-responses
SELECT coalesce(payload->'properties'->>'value', payload->'properties'->>'valueText') AS value,
       payload->'properties'->>'iteration' AS iteration,
       payload->'properties'->>'questionID' AS "question-id",
       payload->'properties'->>'surveyInstanceId' AS "form-instance-id"
  FROM answer
 WHERE payload->'properties'->>'surveyId' = :form-id;

-- :name get-form-instances
SELECT payload->'key'->>'id' AS id,
       payload->'properties'->>'surveyId' AS "form-id",
       payload->'properties'->>'surveyalTime' AS "surveyal-time",
       payload->'properties'->>'submitterName' AS "submitter",
       payload->'properties'->>'collectionDate' AS "submission-date",
       payload->'properties'->>'deviceIdentifier' AS "device-identifier",
       payload->'properties'->>'surveyedLocaleId' AS "data-point-id",
       payload->'properties'->>'surveyedLocaleIdentifier' AS "identifier",
       payload->'properties'->>'surveyedLocaleDisplayName' AS "display-name"
  FROM form_instance
 WHERE payload->'properties'->>'surveyId' = :form-id
