FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY services/*/pom.xml services/*/
COPY . .
RUN mvn clean package -pl services/ai-pricing-service -am -DskipTests

FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /app/services/ai-pricing-service/target/*.jar app.jar
COPY --from=build /app/services/ai-pricing-service/src/main/resources/catboost_model_v1.cbm /app/catboost_model_v1.cbm
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]