# -------------------------------------------------------------
# Stage 1: Build the Spring Boot application using Maven & Java 21
# -------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build jar (skip tests for cloud deployment)
COPY src ./src
RUN mvn clean package -DskipTests -B

# -------------------------------------------------------------
# Stage 2: Minimal, secure Java 21 JRE runtime image
# -------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy executable jar from build stage
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# Render injects PORT dynamically (defaults to 8080)
ENV PORT=8080
EXPOSE 8080

# JVM memory flags optimized for Render free/starter tiers (512MB RAM):
# - MaxRAMPercentage=75.0 prevents container OOM termination
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
