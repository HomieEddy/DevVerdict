# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY review-service/pom.xml review-service/
COPY auth-service/pom.xml auth-service/
COPY gateway/pom.xml gateway/
RUN mvn dependency:go-offline -pl auth-service -am

COPY auth-service/src auth-service/src
RUN mvn clean package -pl auth-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/auth-service/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD ["sh", "-c", "curl -f http://localhost:8083/actuator/health || exit 1"]
ENTRYPOINT ["java", "-jar", "app.jar"]
