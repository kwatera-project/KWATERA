FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn install -N
RUN mvn clean package -pl services/db-migrations -am -DskipTests

FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /app/services/db-migrations/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]