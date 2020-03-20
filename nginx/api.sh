#!/usr/bin/env bash

set -eu

token=$(curl --silent \
	     --data "client_id=curl" \
	     --data "username=${KC_USER}" \
	     --data "password=${KC_PASSWORD}" \
	     --data "grant_type=password" \
	     --data "scope=openid email" \
	     --url "https://kc.akvotest.org/auth/realms/akvo/protocol/openid-connect/token" \
	    | jq -M -r .access_token)

URL="${1}"
shift

curl --silent \
     --header "Content-Type: application/json" \
     --header "Accept: application/vnd.akvo.flow.v2+json" \
     --header "Authorization: Bearer ${token}" \
     --request GET \
     "$@" \
     --url "${URL}"
