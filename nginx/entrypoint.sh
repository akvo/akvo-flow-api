#!/usr/bin/env sh

set -eu

apk add --no-cache \
    wait4ports~=0.2 \
    bash~=5 \
    curl~=7 \
    jq~=1.6

jq -M '. | .credentials.secret = "'"${KC_SECRET}"'"' /secrets/kc-template.json > /secrets/keycloak.json

exec "$@"
