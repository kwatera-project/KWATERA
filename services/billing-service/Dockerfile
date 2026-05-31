FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY services/*/pom.xml services/*/
COPY . .
RUN mvn clean package -pl services/billing-service -am -DskipTests

FROM eclipse-temurin:25-jdk
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/services/billing-service/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]