# ─── Build Stage ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests

# ─── Runtime Stage ────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser  -u 1001 -S appuser -G appgroup

# App jar
COPY --from=builder /build/target/*.jar app.jar

# Persistent uploads directory (mount as Docker volume on EC2)
RUN mkdir -p /app/uploads/agreements && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080
ENV NODE_OPTIONS=--no-deprecation

# Health check — waits for Spring Boot to be ready
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/login || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
