FROM mrsvan/google-cloud-sdk-app-engine-java:jdk8

COPY settings.xml /root/.m2

WORKDIR /app

COPY pom.xml /app/
COPY start.sh /app/
COPY src /app/src/

CMD [ "/app/start.sh" ]