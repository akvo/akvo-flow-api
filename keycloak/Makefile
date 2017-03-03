build:
	docker build -t akvo/flow-api-keycloak .

start:
	docker run \
        --name flow-api-keycloak \
	--detach \
	--publish 8080:8080 \
	akvo/flow-api-keycloak
