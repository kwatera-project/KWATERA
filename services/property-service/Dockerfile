FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn install -N
RUN mvn clean package -pl services/property-service -am -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/services/property-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]