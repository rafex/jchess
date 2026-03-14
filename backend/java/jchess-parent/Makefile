SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

MVN ?= ./mvnw
CLI_JAR ?= jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar
DB ?= jchess.db
PORT ?= 8080

.PHONY: help build build-without-tests test clean verify cli cli-new server

help:
	@echo "Targets:"
	@echo "  make build               -> ./mvnw clean package"
	@echo "  make build-without-tests -> ./mvnw -DskipTests clean package"
	@echo "  make test                -> ./mvnw test"
	@echo "  make clean               -> ./mvnw clean"
	@echo "  make verify              -> ./mvnw verify"
	@echo "  make cli                 -> run CLI help from fat jar"
	@echo "  make cli-new             -> create a demo session from fat jar"
	@echo "  make server              -> start WebSocket server from fat jar"

build:
	$(MVN) -q clean package

build-without-tests:
	$(MVN) -q -DskipTests clean package

test:
	$(MVN) -q test

clean:
	$(MVN) -q clean

verify:
	$(MVN) -q verify

cli: build-without-tests
	java -jar $(CLI_JAR) help

cli-new: build-without-tests
	java -jar $(CLI_JAR) game new --db $(DB)

server: build-without-tests
	java -jar $(CLI_JAR) server start --db $(DB) --port $(PORT)

