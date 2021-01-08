#!/usr/bin/env bash

set -eu

token_response=$(curl --silent \
	     --data "client_id=qsxNP3Nex0wncADQ9Re6Acz6Fa55SuU8" \
	     --data "username=akvo.flow.test.user8@gmail.com" \
	     --data "password=7WqCnqCY6kQJV6YQ7dXT" \
	     --data "grant_type=password" \
	     --data "scope=openid email" \
	     --url "https://akvotest.eu.auth0.com/oauth/token")

token=$(echo "$token_response" | jq -M -r .id_token)

if [ "$token" == "null" ]; then
  echo "No token found. Response was $token_response"
  exit 0
fi

URL="${1}"
shift

curl --verbose \
     --header "Content-Type: application/json" \
     --header "Accept: application/vnd.akvo.flow.v2+json" \
     --header "Authorization: Bearer ${token}" \
     "$@" \
     --url "${URL}" 2>&1 | grep -v "Authorization: Bear"
