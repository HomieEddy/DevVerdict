# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY auth-service/pom.xml auth-service/
RUN mkdir -p auth-service/src
COPY auth-service/src auth-service/src
RUN mvn -pl auth-service -am clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 101 appgroup && adduser -u 101 -G appgroup -s /bin/sh -D appuser
WORKDIR /app
COPY --from=builder /app/auth-service/target/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD ["sh", "-c", "wget -qO- http://localhost:8083/actuator/health || exit 1"]
ENTRYPOINT ["java", "-jar", "app.jar"]
