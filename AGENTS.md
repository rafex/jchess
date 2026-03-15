# AGENTS

## Purpose

This repository is the reference shape for building a backend similar to `jchess`:

- Java 21
- Maven multi-module workspace
- Hexagonal architecture
- No application frameworks
- Standard Java first
- GraalVM-friendly design
- SQLite persistence
- CLI adapter
- HTTP + WebSocket transport
- Jetty 12 through the Ether libraries used in `kiwi` and `housedb`

Use this file as the project-generation playbook when extending this repo or creating another backend with the same style.

## Core Rules

- Preserve hexagonal boundaries.
- Do not introduce Spring, Quarkus, Micronaut, Jakarta EE app stacks, or similar frameworks.
- Prefer Java standard library APIs whenever they are sufficient.
- Use Ether for HTTP/Jetty transport:
  - `dev.rafex.ether.http:ether-http-jetty12`
  - `dev.rafex.ether.json:ether-json`
- Keep domain and application layers free from transport and persistence details.
- Favor explicit code and simple records over abstract hierarchies.
- Keep code GraalVM-conscious:
  - avoid reflection-heavy patterns
  - avoid framework magic
  - keep startup and dependency count low

## Expected Maven Layout

The Java backend lives under `backend/java/<project>-parent`.

Use modules with these responsibilities:

- `*-domain`
  - Pure domain model
  - Enums, records, invariants, value objects
  - No IO, no SQL, no HTTP, no Jetty
- `*-ports`
  - Inbound and outbound ports
  - Use cases and repository/gateway contracts
- `*-application`
  - Application services
  - Orchestration of domain logic
  - Transactional flow and authorization rules at app level
- `*-core`
  - Core algorithms and domain-supporting services
  - Parsers, renderers, evaluators, move generators, search engines, codecs
- `*-adapter-sqlite`
  - SQLite persistence implementation
  - Schema creation and lightweight migrations
- `*-adapter-llm`
  - Optional external AI/HTTP integrations
- `*-transport-api`
  - Shared transport DTOs and presenters
  - Shapes exposed by HTTP and WebSocket
- `*-transport-http`
  - Ether Jetty 12 HTTP handlers and server bootstrap
- `*-transport-websocket`
  - WebSocket sessions, presence, broadcasting
  - If Ether does not cover WS, use Jetty WS directly but keep it isolated here
- `*-tools`
  - Developer/reporting utilities
- `*-cli`
  - Command-line entrypoint
  - Fat jar packaging

For a project modeled after this one, the concrete layout is:

- `jchess-domain`
- `jchess-ports`
- `jchess-application`
- `jchess-core`
- `jchess-adapter-sqlite`
- `jchess-adapter-llm`
- `jchess-transport-api`
- `jchess-transport-http`
- `jchess-transport-websocket`
- `jchess-tools`
- `jchess-cli`

## Parent POM Expectations

When generating a similar backend:

- Use Java 21 with `maven.compiler.release=21`
- Centralize dependency versions in the parent POM
- Keep module list explicit
- Add Maven Wrapper at parent and delegated wrappers in modules if desired
- Prefer a fat jar only in the CLI/application entry module

For Jetty transport, prefer:

- `ether-http-jetty12`
- `ether-json`
- SQLite JDBC when needed

Do not make HTTP handlers depend directly on domain internals if a presenter/DTO layer can isolate them.

## Transport Guidance

### HTTP

Use the same Ether style as `kiwi` and `housedb`:

- `JettyRouteRegistry`
- `JettyServerConfig`
- `JettyServerFactory`
- `NonBlockingResourceHandler`
- `JettyApiResponses`
- `JettyApiErrorResponses`
- `JsonCodecBuilder`

Pattern:

1. Build a `JsonCodec` with Ether.
2. Register handlers in a `JettyRouteRegistry`.
3. Create the server through `JettyServerFactory`.
4. Keep each HTTP resource focused on one base path.

### WebSocket

WebSocket may still use Jetty APIs directly if Ether does not provide a WS abstraction.

Rules:

- Mount WS on the same server process if possible.
- Keep protocol DTOs aligned with HTTP DTOs.
- Version messages explicitly.
- Track presence and side ownership in the transport layer.
- Never trust the frontend on color/turn; validate through app services.

### Transport DTOs

Always keep a shared transport DTO/presenter layer:

- domain objects stay internal
- HTTP/WS payloads are explicit and stable
- web clients should not depend on CLI-oriented output

## Persistence Guidance

For SQLite-backed projects:

- Initialize schema in the adapter
- Use lightweight forward-only migrations
- Add indexes for lookup paths
- Use optimistic locking with a version column when concurrent writes matter
- Store timestamps explicitly

For session-based applications, persist:

- session id
- player ids
- player tokens if the app needs browser/game ownership
- current state snapshot fields
- version
- created/updated timestamps
- event/move history

## CLI Guidance

The CLI should be an adapter, not the system center.

Recommended traits:

- subcommand-based usage
- compatible with scripting
- optional interactive mode
- banner/help can be friendly, but parsing should stay deterministic
- fat jar runnable through `java -jar`

If a backend also exposes web transport, the CLI should be able to start the server.

## Frontend-Ready Backend Checklist

Before calling a backend “ready for web”, verify these exist:

- structured HTTP endpoints
- structured WebSocket protocol
- explicit JSON DTOs
- stable session model
- player identity and ownership checks
- optimistic locking or equivalent concurrency guard
- end-of-game state explicit in API
- browser-friendly move command
  - prefer `from/to/promotion`
  - SAN may exist as secondary notation, not as the main API command

## Testing Expectations

At minimum, add:

- core/domain tests
- application service tests
- persistence adapter tests
- transport serialization or handler tests

For a project like this, verify:

- rules/domain behavior
- persistence round-trip
- optimistic locking
- HTTP contract basics
- WebSocket message flow when possible

## Build And Run

Prefer the wrappers and Makefiles already committed in the repo.

Typical commands:

- root:
  - `make build`
  - `make test`
  - `make verify`
  - `make cli`
  - `make server`
- Java reactor:
  - `backend/java/jchess-parent/Makefile`
  - `backend/java/jchess-parent/mvnw`

## Repository Conventions

- Add `.gitignore` at root, `backend/java`, parent module, and per Maven module when useful.
- Keep README files at:
  - repo root
  - `backend/java`
  - `docs/`
- Keep architecture and protocol docs in `docs/`.
- Use `apply_patch` for edits when acting as the coding agent.

## What To Avoid

- Framework-driven dependency injection
- Anemic “service everywhere” designs with no domain meaning
- Mixing SQL/HTTP/CLI concerns into domain classes
- Exposing persistence entities directly as API payloads
- Ad-hoc JSON strings when Ether JSON is available
- Creating a Jetty setup different from the Ether pattern used in `kiwi` and `housedb` unless there is a strong documented reason

## If Creating A New Similar Project

When asked to generate a new backend similar to this one, follow this order:

1. Create parent Maven reactor under `backend/java/<name>-parent`.
2. Create the standard module set.
3. Add parent dependency management for Java 21, Ether Jetty 12, Ether JSON, tests, and persistence driver.
4. Add wrapper scripts and Makefiles.
5. Create minimal domain, ports, and application skeleton.
6. Add SQLite adapter if sessions/state must persist.
7. Add transport-api module with DTOs/presenters.
8. Add transport-http with Ether handlers.
9. Add transport-websocket if realtime is required.
10. Add CLI fat jar for local workflows.
11. Add tests and docs before calling the scaffold complete.

## Reference

This repo is itself the working reference implementation. When in doubt, prefer reproducing the style and boundaries already present in:

- [backend/java/jchess-parent](/Users/rafex/repository/github/rafex/jchess/backend/java/jchess-parent)
- [docs/architecture.md](/Users/rafex/repository/github/rafex/jchess/docs/architecture.md)
- [README.md](/Users/rafex/repository/github/rafex/jchess/README.md)
