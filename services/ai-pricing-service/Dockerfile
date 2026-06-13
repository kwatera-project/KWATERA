# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml ./
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/config-server/pom.xml services/config-server/pom.xml
COPY services/property-service/pom.xml services/property-service/pom.xml
COPY services/reservation-service/pom.xml services/reservation-service/pom.xml
COPY services/service-registry/pom.xml services/service-registry/pom.xml
COPY services/db-migrations/pom.xml services/db-migrations/pom.xml
COPY services/billing-service/pom.xml services/billing-service/pom.xml
COPY services/ai-pricing-service/pom.xml services/ai-pricing-service/pom.xml
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp -pl services/ai-pricing-service -am dependency:go-offline

COPY . .
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp package -pl services/ai-pricing-service -am -DskipTests

FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /app/services/ai-pricing-service/target/*.jar app.jar
COPY --from=build /app/services/ai-pricing-service/src/main/resources/catboost_model_v1.cbm /app/catboost_model_v1.cbm
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
