build:
	$(MAKE) -C keycloak/ $@
	$(MAKE) -C nginx/ $@
	$(MAKE) -C stub/stub-server/ $@
	@echo "Now: make start"

start:
	$(MAKE) -C keycloak/ $@
	$(MAKE) -C stub/stub-server/ $@
	$(MAKE) -C nginx/ $@
