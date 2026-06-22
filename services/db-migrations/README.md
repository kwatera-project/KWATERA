[← Back to the project README](../../README.md)

# Database Migrations

The Database Migrations service applies the PostgreSQL schema, incremental changes, and demo seed data with Flyway. It runs as a Spring Boot application during the Docker Compose startup.

## Main responsibilities

- Apply versioned Flyway migrations.
- Create and evolve the shared database schema.
- Seed demo users, properties, units, reservations, settlements, and reporting data.

## Default port

`8084` is published by Docker Compose, but this service does not expose an application API.

## Useful local URLs

None. Review startup logs to confirm that Flyway migrations completed successfully.

## Configuration notes

The service requires the PostgreSQL datasource variables supplied by Docker Compose. Migration files are under `src/main/resources/db/migration/`.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
