#!/usr/bin/env bash

set -e

if [[ ! -f "/app/target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated/local_db.bin" ]]; then
    mkdir -p "/app/target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated/"
    wget "https://s3-eu-west-1.amazonaws.com/akvoflow/test-data/local_db.bin" -O "/app/target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated/local_db.bin"
fi

mvn appengine:devserver
