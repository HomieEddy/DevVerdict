FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY review-service/pom.xml review-service/
COPY auth-service/pom.xml auth-service/
COPY gateway/pom.xml gateway/
RUN mvn dependency:go-offline -pl review-service -am

COPY review-service/src review-service/src
RUN mvn clean package -pl review-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/review-service/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
