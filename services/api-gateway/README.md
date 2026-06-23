[← Back to the project README](../../README.md)

# API Gateway

The API Gateway is the public backend entry point for the frontend. It uses Spring Cloud Gateway and Eureka discovery to route requests to the authentication, property, reservation, billing, and AI pricing services.

## Main responsibilities

- Route verified `/api/...` paths to discovered services.
- Centralize the backend address used by the frontend.
- Expose gateway and health actuator information.

## Default port

`8090`

## Useful local URLs

- Health: [http://localhost:8090/actuator/health](http://localhost:8090/actuator/health)

## Configuration notes

The service loads configuration from Config Server and discovers routes through Eureka. Its route definitions are in `src/main/resources/application.yaml`.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
