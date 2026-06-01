# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -g 1001 -S nonroot && adduser -u 1001 -S nonroot -G nonroot
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R nonroot:nonroot /app
USER nonroot
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

