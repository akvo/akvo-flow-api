
.PHONY: clean

build:
	$(MAKE) -C keycloak/ $@
	$(MAKE) -C stub/stub-server/ $@
	$(MAKE) -C nginx/ $@
	@echo "Now: make start"

start:
	$(MAKE) -C stub/stub-server/ $@
	$(MAKE) -C nginx/ $@


start-full:
	$(MAKE) -C keycloak/ $@
	$(MAKE) -C stub/stub-server/ $@
	$(MAKE) -C nginx/ $@

stop:
	docker stop flow-api-stub flow-api-proxy flow-api-keycloak || true

clean: 	stop
	@./util/docker-clean
