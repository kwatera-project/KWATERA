# Stage 2 demo verification checklist

## Scope

Critical Stage 2 flow:

- [ ] Guest can browse properties.
- [ ] Guest can open property or unit details.
- [ ] Guest can select a future date range.
- [ ] System validates availability.
- [ ] Guest can create a reservation.
- [ ] Owner or admin can open reservation overview.
- [ ] Owner or admin can change reservation status.
- [ ] Updated reservation status is visible after change.

## Local startup

- [ ] Docker is running.
- [ ] Local environment starts successfully:

```bash
docker compose -f infra/compose/docker-compose.yml up --build
```

- [ ] Frontend opens at http://localhost:5173.
- [ ] Reservation Service health endpoint works at http://localhost:8080/actuator/health.
- [ ] Swagger UI opens at http://localhost:8080/swagger-ui.html.

## Automated checks

Run from `services/reservation-service`:

```bash
./mvnw -B -ntp test
```

- [ ] Reservation service tests pass.
- [ ] Availability validation is covered.
- [ ] Reservation creation is covered.
- [ ] Status update is covered.
- [ ] Critical Stage 2 flow is covered by `Stage2ReservationFlowTest`.

