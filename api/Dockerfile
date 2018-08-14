FROM openjdk:8-jre-alpine

ENV SECRETS_MOUNT_PATH /secrets
ENV HTTP_PORT 3000
ENV API_ROOT http://localhost:3000/

EXPOSE ${HTTP_PORT}

WORKDIR /app

COPY target/assembly/* /app/

CMD ["java", \
     "-XshowSettings:vm", \
     "-XX:+UnlockExperimentalVMOptions", \
     "-XX:+UseCGroupMemoryLimitForHeap", \
     "-XX:MaxRAMFraction=2", \
     "-XX:+HeapDumpOnOutOfMemoryError", \
     "-XX:HeapDumpPath=/dumps", \
     "-cp", "./*", \
     "org.akvo.flow_api.main"]
