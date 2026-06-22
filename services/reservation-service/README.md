[← Back to the project README](../../README.md)

# Reservation Service

The Reservation Service manages booking availability and the reservation lifecycle. It also supports administrative reservation views, occupancy reporting, scheduled status handling, email notifications, and settlement status events.

## Main responsibilities

- Check availability and create reservations.
- Manage guest and administrator reservation views.
- Track reservation status history and system events.
- Exchange settlement events through Kafka and send local email notifications through Mailpit.

## Default port

`8080`

## Useful local URLs

- Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Gateway paths: `http://localhost:8090/api/v1/reservations` and `/api/availability`

## Configuration notes

The service requires PostgreSQL credentials and `JWT_SECRET`. Kafka, Mailpit, cleanup schedules, and the `Europe/Warsaw` business time zone are configured for the Docker environment.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
