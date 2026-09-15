## Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /project

# Copy Maven wrapper and pom.xml first for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -q

## Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install openssl for RSA key generation at startup
RUN apk add --no-cache openssl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /project/target/quarkus-app/lib/ ./lib/
COPY --from=build /project/target/quarkus-app/*.jar ./
COPY --from=build /project/target/quarkus-app/app/ ./app/
COPY --from=build /project/target/quarkus-app/quarkus/ ./quarkus/

# Startup script: generates RSA keys if missing, then runs Quarkus
COPY --chown=appuser:appgroup docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]
