// Response data format
{
    form: {
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
    },
    formInstances: [
	{
	    id: "3456",
	    dataPointId: "4567",
	    identifier: "hxeq-lb2e-g1n2",
	    displayName: "",
	    deviceIdentifier: "foo",
	    submissionDate: "2017-02-23T03:30:58", // ISO 8601
	    submitter: "John",
	    duration: 123, // seconds
	    responses: {
		// Not sure about this list, as found in QuestionDto.java. I seem to remember
		// that there's a mismatch between question types in form definitions and
		// response types: FREE_TEXT, OPTION, NUMBER, GEO, PHOTO, VIDEO, SCAN, TRACK,
		// NAME, STRENGTH, DATE, CASCADE, GEOSHAPE, SIGNATURE

		// If this form instance is part of a RQG, then all responses will be an array
		// of responses. The index represents the iteration number and null represents
		// missing iterations.

		// question id -> response data
		"597442059": "ABC", // FREE_TEXT
		"432341353": { code: "abc", text: "ABC" }, // OPTION
		"643423135": 1023, // NUMBER
		"532353235": { lat: 51.5432, lon: 24.5432 }, // GEO
		"543643421": "https://example.com/abc.jpg", // PHOTO
		"352315446": "https://example.com/abc.mov", // VIDEO
		"523352135": "2017-02-23T03:30:58", // DATE
		"423352364": [{ code: "abc", text: "ABC" },
			      { code: "def", text: "DEF" }], // CASCADE
		"342352313": {
		    type: "Feature",
		    geometry: {
			type: "Point",
			coordinates: [125.6, 10.1]
		    },
		    properties: {
			name: "Dinagat Islands"
		    }
		}, // GEOJSON
		"353254356": "Zm9vYmFy", // SIGNATURE
		"352342364": "?", // SCAN
		"523521354": "?", // TRACK
		"352343446": "?", // NAME
		"323525436": "?", // STRENGTH
	    }
	},
	{
	    // next form instance
	}
    ]
}
