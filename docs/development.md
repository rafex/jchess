# Development Guide

## Build

From the repository root:

```bash
make build
```

From the Java backend folder:

```bash
cd backend/java
make build
```

From the Maven parent:

```bash
cd backend/java/jchess-parent
./mvnw clean package
```

## Tests

```bash
make test
```

## Wrapper Layout

- `backend/java/jchess-parent/mvnw`: real Maven Wrapper.
- Each Maven module also has `mvnw` and `mvnw.cmd` scripts delegating to the parent wrapper.

## Runtime

Run the CLI:

```bash
make cli
```

Run a demo game:

```bash
make cli-new
```

Run the WebSocket server:

```bash
make server
```

## Notes

- SQLite files are ignored by Git.
- Java 21 is required.
- `sqlite-jdbc` may warn about native access on recent JVMs; use `--enable-native-access=ALL-UNNAMED` if needed.

