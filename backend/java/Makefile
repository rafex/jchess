SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

PARENT_DIR ?= jchess-parent

.PHONY: help build build-without-tests test clean verify cli cli-new server

help:
	@echo "Targets:"
	@echo "  make build               -> mvn clean package"
	@echo "  make build-without-tests -> mvn -DskipTests clean package"
	@echo "  make test                -> mvn test"
	@echo "  make clean               -> mvn clean"
	@echo "  make verify              -> mvn verify"
	@echo "  make cli                 -> run CLI help"
	@echo "  make cli-new             -> create a demo game"
	@echo "  make server              -> start WebSocket server"

build:
	$(MAKE) -C $(PARENT_DIR) build

build-without-tests:
	$(MAKE) -C $(PARENT_DIR) build-without-tests

test:
	$(MAKE) -C $(PARENT_DIR) test

clean:
	$(MAKE) -C $(PARENT_DIR) clean

verify:
	$(MAKE) -C $(PARENT_DIR) verify

cli:
	$(MAKE) -C $(PARENT_DIR) cli

cli-new:
	$(MAKE) -C $(PARENT_DIR) cli-new

server:
	$(MAKE) -C $(PARENT_DIR) server

