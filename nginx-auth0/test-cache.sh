#!/usr/bin/env bash

set -eu

function log {
   echo "$(date +"%T") - INFO - $*"
}

log waiting for docker compose to start
wait4ports nginx=tcp://localhost:8082 upstream=tcp://localhost:3000

function check_status {
  api_result=$(./api.sh "$1" 2>&1)
  if echo "$api_result" | grep -q "$2"; then
    echo "ok"
  else
    echo "$2 not found in response from $1:"
    echo "$api_result"
    exit 1
  fi
}

log check cache miss
check_status "http://localhost:8082/flow/ok" 'X-Cache-Status: MISS'
log check cache miss again
check_status "http://localhost:8082/flow/" 'X-Cache-Status: MISS'
log check cache hit
check_status "http://localhost:8082/flow/" 'X-Cache-Status: HIT'
log check cache hit again
check_status "http://localhost:8082/flow/" 'X-Cache-Status: HIT'
log test passed