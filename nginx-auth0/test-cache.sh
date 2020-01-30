#!/usr/bin/env bash

set -eu

# Checking nginx caching

docker-compose up -d

./test.sh "http://localhost:8082/flow/ok" 2>&1 | grep 'X-Cache-Status: MISS'
./test.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: MISS'
./test.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: HIT'
./test.sh "http://localhost:8082/flow/" 2>&1 | grep 'X-Cache-Status: HIT'

docker-compose down -v
