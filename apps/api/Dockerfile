# syntax=docker/dockerfile:1

# ---- Estágio 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copia primeiro o pom.xml para aproveitar o cache de dependências do Docker.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Estágio 2: runtime ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuário não-root por segurança.
RUN addgroup -S app && adduser -S app -G app \
  && mkdir -p /app/data/videos /app/data/certificates \
  && chown -R app:app /app/data

USER app

COPY --from=build --chown=app:app /build/target/*.jar app.jar

ENV VIDEO_STORAGE_LOCAL_ROOT=/app/data/videos \
    CERTIFICATE_STORAGE_LOCAL_ROOT=/app/data/certificates

EXPOSE 8080
# Render (e similares) injetam PORT; fallback SERVER_PORT / 8080.
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -jar app.jar --server.port=${PORT:-${SERVER_PORT:-8080}}"]
