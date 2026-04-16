FROM maven:3.9.11-eclipse-temurin-23 AS build
WORKDIR /app
COPY . .
RUN mvn install -N
RUN mvn clean package -pl services/service-registry -am -DskipTests

FROM eclipse-temurin:23-jdk
WORKDIR /app
COPY --from=build /app/services/service-registry/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]