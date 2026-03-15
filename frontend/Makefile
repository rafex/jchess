SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

.PHONY: help install dev build preview test e2e

help:
	@echo "Targets:"
	@echo "  make install  -> install frontend dependencies"
	@echo "  make dev      -> start Vite dev server"
	@echo "  make build    -> build frontend"
	@echo "  make preview  -> preview production build"
	@echo "  make test     -> run frontend unit tests"
	@echo "  make e2e      -> run frontend Playwright tests"

install:
	npm install

dev:
	npm run dev

build:
	npm run build

preview:
	npm run preview

test:
	npm run test

e2e:
	npm run test:e2e
