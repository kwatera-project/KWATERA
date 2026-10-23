[← Back to the project README](../../README.md)

# Reservation Service

The Reservation Service manages booking availability, the reservation lifecycle, and reservation-based conversations. It also supports administrative reservation views, occupancy reporting, scheduled status handling, email notifications, and settlement status events.

## Main responsibilities

- Check availability and create reservations.
- Manage guest and administrator reservation views.
- Track reservation status history and system events.
- Create and expose one guest-owner conversation per eligible reservation.
- Exchange settlement events through Kafka and send local email notifications through Mailpit.

## Default port

`8080`

## Useful local URLs

- Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Gateway paths: `http://localhost:8090/api/v1/reservations`, `/api/v1/conversations`, and `/api/availability`

## Conversation API

All conversation endpoints require a JWT. The user id is taken from the authenticated token, never from request data. A conversation snapshots the reservation guest and the current unit owner when it is first created; only those two users can access it. Administrators have no implicit access.

| Method | Path | Purpose |
|---|---|---|
| `PUT` | `/api/v1/reservations/{reservationId}/conversation` | Idempotently get or create the reservation conversation |
| `GET` | `/api/v1/conversations` | List the current user's available conversations |
| `GET` | `/api/v1/conversations/{conversationId}` | Read one available conversation |

`PENDING` and `CONFIRMED` reservations remain available. `BLOCKED` reservations are rejected. `COMPLETED` and `CANCELLED` reservations remain available through the configured retention boundary (90 days by default), measured from the later of the lifecycle date and `last_message_at`. Expired conversations are omitted from lists and return `409 Conflict` when addressed directly. Missing and non-participant resources both return `404 Not Found` to prevent identifier probing.

Migration `V35` reconstructs missing legacy cancellation timestamps only from explicit cancellation history or audit events. If no reliable cancellation evidence exists, `cancelled_at` remains `NULL` and conversation access is denied with `409 Conflict`; `updated_at` is intentionally not used as a substitute. Such a record may be repaired only when an externally verified cancellation timestamp is available.

## Configuration notes

The service requires PostgreSQL credentials and `JWT_SECRET`. Kafka, Mailpit, cleanup schedules, and the `Europe/Warsaw` business time zone are configured for the Docker environment. Conversation retention can be overridden with `KWATERA_CONVERSATION_RETENTION_DAYS`. Shared service-to-service HTTP defaults are a 2-second connect timeout and a 5-second read timeout; use `KWATERA_HTTP_CONNECT_TIMEOUT` and `KWATERA_HTTP_READ_TIMEOUT` to override them.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
