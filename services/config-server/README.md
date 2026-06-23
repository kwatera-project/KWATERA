[← Back to the project README](../../README.md)

# Config Server

The Config Server provides shared Spring configuration to the Java services. It runs in native mode and reads configuration files from the repository's `config-repo/` directory.

## Main responsibilities

- Serve centralized Spring configuration.
- Provide shared datasource, Eureka, management, and project metadata.
- Start before services that import remote configuration.

## Default port

`8888`

## Useful local URLs

- Health: [http://localhost:8888/actuator/health](http://localhost:8888/actuator/health)

## Configuration notes

Docker Compose mounts `config-repo/` read-only at `/config-repo`, matching the native search location in `application.yaml`.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
