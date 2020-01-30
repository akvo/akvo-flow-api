#!/usr/bin/env bash

set -eu

token=$(curl --silent \
	     --data "client_id=qsxNP3Nex0wncADQ9Re6Acz6Fa55SuU8" \
	     --data "username=${AUTH0_USER}" \
	     --data "password=${AUTH0_PASSWORD}" \
	     --data "grant_type=password" \
	     --data "scope=openid email" \
	     --url "https://akvotest.eu.auth0.com/oauth/token" \
	    | jq -M -r .id_token)

URL="${1}"
shift

curl --verbose \
     --header "Content-Type: application/json" \
     --header "Accept: application/vnd.akvo.flow.v2+json" \
     --header "Authorization: Bearer ${token}" \
     --request GET \
     "$@" \
     --url "${URL}" | jq -M
