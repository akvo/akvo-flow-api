#!/usr/bin/env bash

set -eu

wait4ports nginx=tcp://localhost:8082 upstream=tcp://localhost:3000

./api.sh "http://localhost:8082/flow/ok" 2>&1 | grep 'X-Cache-Status: MISS'
./api.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: MISS'
./api.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: HIT'
./api.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: HIT'
