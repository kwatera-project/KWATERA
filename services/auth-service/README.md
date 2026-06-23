[← Back to the project README](../../README.md)

# Auth Service

The Auth Service manages users, authentication, profiles, password reset, newsletter subscriptions, and administrator user reporting. It issues and validates JWT-based authentication data used across the platform.

## Main responsibilities

- Register and authenticate users.
- Manage user profiles and role-aware administrator views.
- Handle password-reset and newsletter email flows.
- Integrate with the property service for selected user-related data.

## Default port

`8081`

## Useful local URLs

- Health: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
- Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- Gateway paths: `http://localhost:8090/api/auth`, `/api/newsletter`, and `/api/admin/users`

## Configuration notes

The service requires PostgreSQL credentials and `JWT_SECRET`. Docker Compose also supplies Mailpit settings and `SPRING_AI_OPENAI_API_KEY` for its OpenAI-compatible Groq integration.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
