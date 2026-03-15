SHELL := /bin/bash
.ONESHELL:
.SHELLFLAGS := -euo pipefail -c

.PHONY: help install dev build preview

help:
	@echo "Targets:"
	@echo "  make install  -> install frontend dependencies"
	@echo "  make dev      -> start Vite dev server"
	@echo "  make build    -> build frontend"
	@echo "  make preview  -> preview production build"

install:
	npm install

dev:
	npm run dev

build:
	npm run build

preview:
	npm run preview
