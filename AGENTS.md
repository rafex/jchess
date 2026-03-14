# AGENTS

## Overview

This repository hosts `jchess`, a Java 21 chess engine and backend workspace.

## Principles

- Preserve the hexagonal architecture.
- Prefer standard Java libraries first.
- Do not introduce application frameworks.
- Keep GraalVM compatibility in mind.
- Favor explicit, readable code over clever abstractions.

## Maven Layout

The backend lives in `backend/java/jchess-parent` and is split into:

- `jchess-domain`
- `jchess-ports`
- `jchess-application`
- `jchess-core`
- `jchess-adapter-sqlite`
- `jchess-adapter-llm`
- `jchess-transport-websocket`
- `jchess-tools`
- `jchess-cli`

## Development Commands

Use the wrappers and Makefiles already committed in the repo:

- Root: `make build`, `make test`, `make server`
- Java workspace: `backend/java/Makefile`
- Parent reactor: `backend/java/jchess-parent/Makefile`

## Coding Notes

- Chess rules, notation, search, and board logic belong in `jchess-core` and `jchess-domain`.
- Application orchestration belongs in `jchess-application`.
- Persistence adapters belong in `jchess-adapter-*`.
- Transport-specific code belongs in `jchess-transport-*`.
- Avoid leaking infrastructure concerns into domain objects.

