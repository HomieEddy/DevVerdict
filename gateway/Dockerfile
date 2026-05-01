FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY review-service/pom.xml review-service/
COPY auth-service/pom.xml auth-service/
COPY gateway/pom.xml gateway/
RUN mvn dependency:go-offline -pl gateway -am

COPY gateway/src gateway/src
RUN mvn clean package -pl gateway -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/gateway/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
