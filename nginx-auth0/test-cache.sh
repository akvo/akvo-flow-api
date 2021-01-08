#!/usr/bin/env bash

set -eu

function log {
   echo "$(date +"%T") - INFO - $*"
}

log waiting for docker compose to start
wait4ports nginx=tcp://localhost:8082 upstream=tcp://localhost:3000


log check cache miss
./api.sh "http://localhost:8082/flow/ok"
./api.sh "http://localhost:8082/flow/ok" 2>&1 | grep 'X-Cache-Status: MISS'
log check cache miss again
./api.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: MISS'
log check cache hit
./api.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: HIT'
log check cache hit again
./api.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: HIT'
log test passed