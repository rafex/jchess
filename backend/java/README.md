# Java Backend

This folder contains the Java 21 backend for `jchess`.

## Structure

- `jchess-parent/`: Maven multi-module parent reactor.
- `Makefile`: Java backend entrypoint for build and runtime commands.

## Common Commands

```bash
make build
make test
make cli
make cli-new
make server
```

## Maven

Use the wrapper from `jchess-parent`:

```bash
cd jchess-parent
./mvnw clean package
```

