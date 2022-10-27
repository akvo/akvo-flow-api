#!/usr/bin/env bash

set -eux

AUTH_FILE=/tmp/auth.json

if [[ ! -f "${AUTH_FILE}" ]]; then
    curl --silent \
	     --data "client_id=qsxNP3Nex0wncADQ9Re6Acz6Fa55SuU8" \
	     --data "username=akvo.flow.test.user8@gmail.com" \
	     --data "password=7WqCnqCY6kQJV6YQ7dXT" \
	     --data "grant_type=password" \
	     --data "scope=openid email" \
	     --url "https://akvotest.eu.auth0.com/oauth/token" \
         > "${AUTH_FILE}"
fi

token=$(jq -M -r .id_token "${AUTH_FILE}")

if [[ -z "${token}" ||  "${token}" == "null" ]]; then
  echo "Auth token not found."
  exit 1
fi

URL="${1}"
shift

curl --verbose \
     --header "Content-Type: application/json" \
     --header "Accept: application/vnd.akvo.flow.v2+json" \
     --header "Authorization: Bearer ${token}" \
     "$@" \
     --url "${URL}" 2>&1
