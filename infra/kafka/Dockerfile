# syntax=docker/dockerfile:1.7
# Kafka uses Alpine/musl, so retain Java 21 with its vendor-confirmed fix.
FROM eclipse-temurin:21-jre-alpine@sha256:5d7689f47d670aca766b9503a70bccc2d73b4eb9221ae8f714ae958313c4ce70 AS jdk
RUN java -version \
    && . "$JAVA_HOME/release" \
    && [ "$IMPLEMENTOR" = "Eclipse Adoptium" ] \
    && [ "$JAVA_VERSION" = "21.0.13.1" ]

# Preserve the existing Kafka 4.2.0 distribution, scripts and container settings.
FROM apache/kafka:latest@sha256:9516fb7634bad307d17c33b589fde9023003b0cb761374f500002b980a3149b9 AS runtime
USER root
RUN rm -rf /opt/java/openjdk
COPY --from=jdk /opt/java/openjdk /opt/java/openjdk
ENV JAVA_VERSION=jdk-21.0.13.1+2
USER appuser
RUN java -version
