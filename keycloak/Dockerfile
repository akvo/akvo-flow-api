FROM jboss/keycloak:2.5.4.Final

ENV KEYCLOAK_USER=admin
ENV KEYCLOAK_PASSWORD=admin

ADD akvo.json /tmp/akvo.json

CMD ["-b", "0.0.0.0", "-Dkeycloak.migration.action=import", \
     "-Dkeycloak.migration.provider=singleFile", \
     "-Dkeycloak.migration.file=/tmp/akvo.json", \
     "-Dkeycloak.migration.strategy=OVERWRITE_EXISTING"]
