FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build
COPY jchess-parent/ ./jchess-parent/

WORKDIR /build/jchess-parent
RUN chmod +x mvnw && ./mvnw -q -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S jchess && adduser -S jchess -G jchess
WORKDIR /app

COPY --from=build /build/jchess-parent/jchess-cli/target/jchess-cli-0.1.0-SNAPSHOT.jar jchess.jar

RUN mkdir -p /app/data && chown -R jchess:jchess /app

USER jchess

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "jchess.jar", "server", "start", "--db", "/app/data/jchess.db", "--port", "8080"]
