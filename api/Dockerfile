FROM openjdk:8-jre-alpine

ENV SECRETS_MOUNT_PATH /secrets
ENV HTTP_PORT 3000
ENV API_ROOT http://localhost:3000/

EXPOSE ${HTTP_PORT}

WORKDIR /app

COPY target/assembly/* /app/

COPY run.sh /app/run.sh

CMD ["./run.sh"]
