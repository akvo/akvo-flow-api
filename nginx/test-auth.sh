#!/usr/bin/env bash

set -eu

wait4ports api-proxy=tcp://localhost:8081 upstream=tcp://localhost:3000

./api.sh "http://localhost:8081/flow/ok" | grep 'X-Akvo-Email'
