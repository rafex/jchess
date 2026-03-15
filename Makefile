SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

BACKEND_DIR ?= backend

.PHONY: help build build-without-tests test clean verify cli cli-new server frontend-install frontend-dev frontend-build frontend-test frontend-e2e

help:
	@echo "Targets:"
	@echo "  make build               -> build backend workspace"
	@echo "  make build-without-tests -> package backend without tests"
	@echo "  make test                -> run backend tests"
	@echo "  make clean               -> clean backend outputs"
	@echo "  make verify              -> run backend verification"
	@echo "  make cli                 -> run CLI help"
	@echo "  make cli-new             -> create a demo game"
	@echo "  make server              -> start backend HTTP/WebSocket server"
	@echo "  make frontend-install    -> install frontend dependencies"
	@echo "  make frontend-dev        -> start frontend Vite server"
	@echo "  make frontend-build      -> build frontend"
	@echo "  make frontend-test       -> run frontend unit tests"
	@echo "  make frontend-e2e        -> run frontend Playwright tests"

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

frontend-install:
	$(MAKE) -C frontend install

frontend-dev:
	$(MAKE) -C frontend dev

frontend-build:
	$(MAKE) -C frontend build

frontend-test:
	$(MAKE) -C frontend test

frontend-e2e:
	$(MAKE) -C frontend e2e
