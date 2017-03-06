
FLOW_API_STUB_IP := $(shell docker inspect flow-api-stub | jq .[].NetworkSettings.Networks.bridge.IPAddress | sed 's/"//g')

build:
	docker build -t akvo/flow-api-proxy .


start: 	LUMEN_KEYCLOAK_IP := $(shell docker inspect lumen-keycloak | jq .[].NetworkSettings.Networks.bridge.IPAddress | sed 's/"//g')
start:
	docker run \
	--detach \
	--name flow-api-proxy \
	--env FLOW_API_URL="http://$(FLOW_API_STUB_IP):3333" \
	--env TOKEN_INTROSPECTION_URL="http://$(LUMEN_KEYCLOAK_IP):8080/auth/realms/akvo/protocol/openid-connect/token/introspect" \
	--env CLIENT_ID="flow-api" \
	--env CLIENT_SECRET="cd1d05f4-c663-4f54-9611-c1f1d2890812" \
	--publish 8081:8081 \
	akvo/flow-api-proxy


start-full: KEYCLOAK_IP := $(shell docker inspect flow-api-keycloak | jq .[].NetworkSettings.Networks.bridge.IPAddress | sed 's/"//g')
start-full:
	docker run \
	--detach \
	--name flow-api-proxy \
	--env FLOW_API_URL="http://$(FLOW_API_STUB_IP):3333" \
	--env TOKEN_INTROSPECTION_URL="http://$(KEYCLOAK_IP):8080/auth/realms/akvo/protocol/openid-connect/token/introspect" \
	--env CLIENT_ID="flow-api" \
	--env CLIENT_SECRET="cd1d05f4-c663-4f54-9611-c1f1d2890812" \
	--publish 8081:8081 \
	akvo/flow-api-proxy


debug:	KEYCLOAK_IP := $(shell docker inspect flow-api-keycloak | jq .[].NetworkSettings.Networks.bridge.IPAddress | sed 's/"//g')
debug:
	docker run \
	--name flow-api-proxy \
	--env FLOW_API_URL="http://$(FLOW_API_STUB_IP):3333" \
	--env TOKEN_INTROSPECTION_URL="http://$(KEYCLOAK_IP):8080/auth/realms/akvo/protocol/openid-connect/token/introspect" \
	--env CLIENT_ID="flow-api" \
	--env CLIENT_SECRET="cd1d05f4-c663-4f54-9611-c1f1d2890812" \
	--publish 8081:8081 \
	--entrypoint /bin/sh \
	--tty \
	--interactive \
	akvo/flow-api-proxy
