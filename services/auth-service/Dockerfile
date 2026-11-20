# syntax=docker/dockerfile:1.7
FROM mcr.microsoft.com/openjdk/jdk:25-ubuntu AS jdk
# Reject stale/unpatched tags before either Maven or the application can use Java.
RUN java -version \
    && . "$JAVA_HOME/release" \
    && [ "$IMPLEMENTOR" = "Microsoft" ] \
    && [ "${JAVA_VERSION%%.*}" = "25" ] \
    && dpkg --compare-versions "$JAVA_VERSION" ge "25.0.5.1"

# Copy Maven only; the donor image's Temurin JVM is never executed or included.
FROM maven:3.9.11-eclipse-temurin-25 AS maven
FROM jdk AS build
COPY --from=maven /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"
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
COPY services/review-service/pom.xml services/review-service/pom.xml
COPY services/ai-service/pom.xml services/ai-service/pom.xml
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp -pl services/auth-service -am dependency:go-offline

COPY . .
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -B -ntp package -pl services/auth-service -am -DskipTests

FROM jdk AS runtime
WORKDIR /app
COPY --from=build /app/services/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
