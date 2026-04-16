FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .

# TEST STAGE
FROM base AS test
RUN mvn test -pl services/auth-service -am

# BUILD STAGE
FROM test AS build
RUN mvn package -pl services/auth-service -am -DskipTests

# RUNTIME
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /app/services/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
