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
	    parentId: "0"
	    name: "A Folder",
	    createdAt: "2017-02-23T03:30:58",
	    modifiedAt: "2017-02-23T03:30:58",
	    foldersUrl: "https://.../folders?parentId=123",
	    surveysUrl: "https://.../folders?parentId=123",
	}, {
	    id: "321",
	    parentId: "123"
	    name: "Another Folder",
	    createdAt: "2017-02-23T03:30:58",
	    modifiedAt: "2017-02-23T03:30:58",
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
	    folderId: "123
	    name: "A Survey",
	    createdAt: "2017-02-23T03:30:58",
	    modifiedAt: "2017-02-23T03:30:58",
	    surveyUrl: "https://.../survey/223",
	}, {
	    id: "221",
	    folderId: "123"
	    name: "Another Survey",
	    createdAt: "2017-02-23T03:30:58",
	    modifiedAt: "2017-02-23T03:30:58",
	    surveyUrl: "https://.../survey/221",
	}
    ]
}

// Full survey definition
// /instance/{instanceId}/survey/223

{
    id: "223",
    name: "A Survey",
    createdAt: "2017-02-23T03:30:58",
    modifiedAt: "2017-02-23T03:30:58",
    forms: [
	{
	    id: "1234",
	    surveyId: "2345",
	    name: "A Form",
	    createdAt: "2017-02-23T03:30:58",
	    modifiedAt: "2017-02-23T03:30:58",
	    questionGroups: [
		{
		    id: "3456",
		    name: "A Question Group",
		    isRepeatable: false,
		    createdAt: "2017-02-23T03:30:58",
		    modifiedAt: "2017-02-23T03:30:58",
		    questions: [
			{
			    id: "4567",
			    name: "A Question?",
			    type: "FREE_TEXT",
			    createdAt: "2017-02-23T03:30:58",
			    modifiedAt: "2017-02-23T03:30:58",
			},
			{
			    id: "3765",
			    name: "Another Question?",
			    type: "VIDEO",
			    createdAt: "2017-02-23T03:30:58",
			    modifiedAt: "2017-02-23T03:30:58",
			}
		    ]
		},
		{
		    id: "2543",
		    name: "Another Question Group",
		    isRepeatable: true,
		    createdAt: "2017-02-23T03:30:58",
		    modifiedAt: "2017-02-23T03:30:58",
		    questions: [
			{
			    id: "3444",
			    name: "Yet Another Question?",
			    type: "OPTION",
			    createdAt: "2017-02-23T03:30:58",
			    modifiedAt: "2017-02-23T03:30:58",
			}
		    ]
		}
	    ]
	}
    ]
}

// Get response data as a bulk "file"

// Request /instance/{instanceId}/responses/{surveyId}/{formId}
// Response Headers:
// * 301 Moved Permanently
// * Location https://api/akvo.org/flow/{instanceId}/response-data/{id}
// where
// * "Pending"
//   * 202 Accepted
// * "File Available"
//   * 200 OK
//   * Content-Encoding: chunked, gzipped
// * "Error"
//   * 4XX (timeout, not-found, other error)
