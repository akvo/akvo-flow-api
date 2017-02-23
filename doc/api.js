/*

   Flow API sketch

   * Only GET requests for now at https://api.akvo.org/flow/
   * Request headers
     * Authorization: Bearer <token>
     * Content-Type: application/json
   * Response headers
     * Content-Type: application/json
     * Content-Encoding: gzip (chunked?)

*/

// List folders:
// /instance/{instanceId}/folders?parentId={parentId}

{
    folders: [
	{
	    id: "123",
	    name: "A Folder",
	    foldersUrl: "https://.../folders?parentId=123",
	    surveysUrl: "https://.../folders?parentId=123",
	}, {
	    id: "321",
	    name: "Another Folder",
	    foldersUrl: "https://.../folders?parentId=321",
	    surveysUrl: "https://.../folders?parentId=321",
	}
    ]
}

// List surveys
// /instance/{instanceId}/surveys?folderId={folderId}

{
    surveys: [
	{
	    id: "223"
	    name: "A Survey",
	    surveyUrl: "https://.../survey/223",
	}, {
	    id: "221",
	    name: "Another Survey",
	    surveyUrl: "https://.../survey/221",
	}
    ]
}

// Full survey definition
// /instance/{instanceId}/survey/223

{
    id: "223",
    name: "A Survey",
    forms: [
	{
	    id: "1234",
	    surveyId: "2345",
	    name: "A Form",
	    questionGroups: [
		{
		    id: "3456",
		    name: "A Question Group",
		    isRepeatable: false,
		    questions: [
			{
			    id: "4567"
			    name: "A Question?"
			    type: "FREE_TEXT"
			},
			{
			    id: "3765"
			    name: "Another Question?"
			    type: "VIDEO"
			}
		    ]
		},
		{
		    id: "2543",
		    name: "Another Question Group",
		    isRepeatable: true,
		    questions: [
			{
			    id: "3444",
			    name: "Yet Another Question?",
			    type: "OPTION"
			}
		    ]
		}
	    ]
	}
    ]
}
