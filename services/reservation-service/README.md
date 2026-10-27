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

### Persistent messages

`POST /api/v1/conversations/{conversationId}/messages` accepts `{"content":"Hello, what time can we check in?"}` and returns `201` with `id`, `conversationId`, `senderUserId`, `content`, and an ISO-8601 `createdAt` instant. The sender comes from the authenticated JWT; the server generates the UUID and timestamp. Content is normalized with Java `String.strip()` and must contain 1–4,000 Java characters after normalization. Internal whitespace and line breaks are preserved. Missing, null, blank, or overlong content returns `400`; sending never creates a conversation.

`GET /api/v1/conversations/{conversationId}/messages?limit=50` returns `200` with `items`, `nextCursor`, and `hasMore`. Items are newest first, ordered by `(createdAt DESC, id DESC)`. The default limit is 50; valid limits are 1–100. Pass `nextCursor` unchanged as the `cursor` query parameter to retrieve the next older page. Cursors include the conversation, exact microsecond timestamp and UUID boundary. A newer insertion does not shift older pages. Empty and final pages return `nextCursor: null` and `hasMore: false`. Invalid limits, malformed cursors and cursors from another conversation return `400` after access is checked.

Both operations use the same participant and lifecycle rules as conversation access: `401` without authentication, equivalent `404` responses for missing or inaccessible conversations, and `409` for an unavailable lifecycle. Expired conversations cannot be revived by attempting a send. A transaction locks the conversation during sends, persists the message and advances `lastMessageAt` together without moving it backwards. Migration `V38` creates `conversation_messages` with required fields, length constraints, a conversation foreign key and the ordered history index; sender IDs have no cross-service user foreign key. Gateway routing and service security already cover both paths. Swagger UI documents the schemas and examples.

## Configuration notes

The service requires PostgreSQL credentials and `JWT_SECRET`. Kafka, Mailpit, cleanup schedules, and the `Europe/Warsaw` business time zone are configured for the Docker environment. Conversation retention can be overridden with `KWATERA_CONVERSATION_RETENTION_DAYS`. Shared service-to-service HTTP defaults are a 2-second connect timeout and a 5-second read timeout; use `KWATERA_HTTP_CONNECT_TIMEOUT` and `KWATERA_HTTP_READ_TIMEOUT` to override them.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).

## Real-time messaging

See [WebSocket/STOMP integration](../../docs/messaging.md) for authentication, destinations, recovery, limitations and a runnable gateway smoke test.
