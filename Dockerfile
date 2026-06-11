# Build React frontend
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY ygc-web/package.json ygc-web/package-lock.json* ./
RUN npm ci || npm install
COPY ygc-web/ .
RUN npm run build

# Build Spring Boot backend
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=builder /build/target/*.jar app.jar

RUN mkdir -p /app/uploads/agreements && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/login || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

# React build output available for nginx via:
# COPY --from=frontend /frontend/dist /usr/share/nginx/html
# (mounted in docker-compose.prod.yml from ygc-web/dist)
