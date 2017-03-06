#!/usr/bin/env bash

set -eu

ACCESS_TOKEN=$(curl -s \
	            -d "client_id=curl" \
	            -d "username=demo1" \
		    -d "password=akvo123" \
		    -d "grant_type=password" \
		    "http://localhost:8080/auth/realms/akvo/protocol/openid-connect/token" | \
	            jq .access_token | \
		    sed 's/"//g');

curl -s -H "Authorization: Bearer ${ACCESS_TOKEN}" http://localhost:8081/flow/folders?parentId=0
