[← Back to the project README](../../README.md)

# Service Registry

The Service Registry is the Eureka server used by KWATERA's Spring services. It lets the API Gateway and service-to-service clients locate running service instances by application name.

## Main responsibilities

- Register discoverable Spring services.
- Provide service discovery to the API Gateway and internal clients.
- Display the current registry in the Eureka dashboard.

## Default port

`8761`

## Useful local URLs

- Eureka dashboard: [http://localhost:8761](http://localhost:8761)
- Health: [http://localhost:8761/actuator/health](http://localhost:8761/actuator/health)

## Configuration notes

The registry imports configuration from Config Server and does not register itself as a Eureka client.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
