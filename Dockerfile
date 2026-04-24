# ─────────────────────────────────────────────────────────────────────────────
# Stage 1: Build — assemble the fat JAR using Gradle
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build descriptor files first to cache the dependency
# download layer separately from the source code.
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

# Download dependencies (cached as a separate layer)
RUN ./gradlew dependencies --no-daemon

# Copy source and build the fat JAR
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Runtime — minimal JRE image with a non-root user
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create a non-root group and user for security hardening
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Run as non-root user
USER appuser

# Expose the application HTTP port
EXPOSE 8080

# Runtime environment variables — injected at container start time.
# Do NOT embed real credentials in this file or any image layer.
ENV AWS_REGION=""
ENV AWS_ACCESS_KEY_ID=""
ENV AWS_SECRET_ACCESS_KEY=""
ENV DYNAMODB_TABLE_NAME=""

# Launch the application directly (no shell wrapper)
ENTRYPOINT ["java", "-jar", "app.jar"]
