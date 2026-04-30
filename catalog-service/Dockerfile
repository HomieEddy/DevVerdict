FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY review-service/pom.xml review-service/
COPY gateway/pom.xml gateway/
RUN mvn dependency:go-offline -pl catalog-service -am

COPY catalog-service/src catalog-service/src
RUN mvn clean package -pl catalog-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/catalog-service/target/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
