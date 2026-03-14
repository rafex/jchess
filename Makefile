SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

BACKEND_DIR ?= backend

.PHONY: help build build-without-tests test clean verify cli cli-new server

help:
	@echo "Targets:"
	@echo "  make build               -> build backend workspace"
	@echo "  make build-without-tests -> package backend without tests"
	@echo "  make test                -> run backend tests"
	@echo "  make clean               -> clean backend outputs"
	@echo "  make verify              -> run backend verification"
	@echo "  make cli                 -> run CLI help"
	@echo "  make cli-new             -> create a demo game"
	@echo "  make server              -> start backend WebSocket server"

build:
	$(MAKE) -C $(BACKEND_DIR) build

build-without-tests:
	$(MAKE) -C $(BACKEND_DIR) build-without-tests

test:
	$(MAKE) -C $(BACKEND_DIR) test

clean:
	$(MAKE) -C $(BACKEND_DIR) clean

verify:
	$(MAKE) -C $(BACKEND_DIR) verify

cli:
	$(MAKE) -C $(BACKEND_DIR) cli

cli-new:
	$(MAKE) -C $(BACKEND_DIR) cli-new

server:
	$(MAKE) -C $(BACKEND_DIR) server

