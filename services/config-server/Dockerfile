FROM maven:3.9.11-eclipse-temurin-23 AS build
WORKDIR /app
COPY . .
RUN mvn install -N
RUN mvn clean package -pl services/config-server -am -DskipTests

FROM eclipse-temurin:23-jdk
WORKDIR /app
COPY --from=build /app/services/config-server/target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]