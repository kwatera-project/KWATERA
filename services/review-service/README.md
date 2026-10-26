[← Back to the project README](../../README.md)

# Review Service

The Review Service manages guest reviews and star ratings for completed stays. 

## Main responsibilities

- Allow guests to submit star ratings (1–5) and text reviews exclusively after a completed reservation.
- Link reviews to specific reservations, rooms, or properties.
- Browse, sort (by date and rating), and filter reviews by star count.
- Calculate average star ratings for rooms, aggregate room ratings at the property level, and compute overall owner ratings.
- Enable owners to add public responses to reviews.

## Default port

`8089`

## Useful local URLs

- Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Gateway paths: `http://localhost:8090/api/reviews`

## Configuration notes

The service requires PostgreSQL credentials and `JWT_SECRET`. Kafka, Mailpit, cleanup schedules, and the `Europe/Warsaw` business time zone are configured for the Docker environment.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
