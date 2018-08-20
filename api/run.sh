#!/usr/bin/env sh

set -eu

hostname=$(hostname)
ts=$(date +%s)
dump_filename="${hostname}-${ts}.hprof"

java -XshowSettings:vm \
     -XX:+UnlockExperimentalVMOptions \
     -XX:+UseCGroupMemoryLimitForHeap \
     -XX:MaxRAMFraction=2 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath="/dumps/${dump_filename}" \
     -cp "./*" \
     org.akvo.flow_api.main
