# Shared Dockerfile for every Spring Boot module.
#
# One file is used for all of them because the build is identical; only the module changes:
#   docker build --build-arg MODULE=account-service -t projet-adrea/account-service .
# docker-compose.yml passes MODULE for each service.
#
# Multi-stage: the JDK and the Maven cache stay in the builder, the runtime image carries only a JRE
# and the jar.

# ---------- build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Wrapper and POMs first: this layer is cached as long as no dependency changes.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY discovery-service/pom.xml discovery-service/
COPY config-server/pom.xml config-server/
COPY api-gateway/pom.xml api-gateway/
COPY account-service/pom.xml account-service/
COPY transaction-service/pom.xml transaction-service/
COPY event-consumer/pom.xml event-consumer/
COPY assistant-service/pom.xml assistant-service/
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline -DskipTests || true

COPY . .

ARG MODULE
RUN test -n "$MODULE" || (echo "MODULE build-arg is required" && false) \
    && ./mvnw -B -pl "$MODULE" -am -DskipTests package \
    && cp "$MODULE"/target/*.jar /build/application.jar

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Never run the service as root.
RUN addgroup -S bank && adduser -S bank -G bank
COPY --from=builder --chown=bank:bank /build/application.jar application.jar
USER bank

EXPOSE 8080
ENV JAVA_OPTS=""

# Containers get their configuration from the environment, never from a baked-in file.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/application.jar"]
