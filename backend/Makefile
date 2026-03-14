SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

JAVA_DIR ?= java

.PHONY: help build build-without-tests test clean verify cli cli-new server

help:
	@echo "Targets:"
	@echo "  make build               -> build Java backend"
	@echo "  make build-without-tests -> package Java backend without tests"
	@echo "  make test                -> run Java backend tests"
	@echo "  make clean               -> clean Java backend"
	@echo "  make verify              -> verify Java backend"
	@echo "  make cli                 -> run CLI help"
	@echo "  make cli-new             -> create a demo game"
	@echo "  make server              -> start WebSocket server"

build:
	$(MAKE) -C $(JAVA_DIR) build

build-without-tests:
	$(MAKE) -C $(JAVA_DIR) build-without-tests

test:
	$(MAKE) -C $(JAVA_DIR) test

clean:
	$(MAKE) -C $(JAVA_DIR) clean

verify:
	$(MAKE) -C $(JAVA_DIR) verify

cli:
	$(MAKE) -C $(JAVA_DIR) cli

cli-new:
	$(MAKE) -C $(JAVA_DIR) cli-new

server:
	$(MAKE) -C $(JAVA_DIR) server

