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
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
