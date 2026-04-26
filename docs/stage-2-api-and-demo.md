# Stage 2 API contract and local demo setup

## Local startup

Run the local environment from the repository root:

```bash
docker compose -f infra/compose/docker-compose.yml up --build
```

## OpenAPI / Swagger UI

After startup, API documentation is available at:

- Auth Service: http://localhost:8081/swagger-ui.html
- Property Service: http://localhost:8083/swagger-ui.html
- Reservation Service: http://localhost:8080/swagger-ui.html

Raw OpenAPI JSON is available at:

- Auth Service: http://localhost:8081/v3/api-docs
- Property Service: http://localhost:8083/v3/api-docs
- Reservation Service: http://localhost:8080/v3/api-docs

## Stage 2 demo flow

1. Guest opens the frontend.
2. Guest browses available properties.
3. Guest opens property details and available units.
4. Guest selects dates.
5. System validates unit availability.
6. Guest creates a reservation.
7. Owner or admin opens the reservation overview.
8. Owner or admin reviews reservations and their statuses.

## Roles used in the MVP

- `ADMIN`: can access administrative reservation overview.
- `OWNER`: can access owner-side reservation overview for owned units.
- `GUEST`: guest-facing user role for browsing and reservation flow.

## Maintenance rule

Every new or changed REST endpoint should be checked in Swagger UI before opening a pull request. If the generated description is unclear, the author should add OpenAPI annotations or improve DTO names and validation.
