[← Back to the project README](../../README.md)

# Billing Service

The Billing Service manages settlements, payment checkout, invoices, payment status, and utility meter readings. It integrates with Stripe for payments and the OCR Service for assisted water meter reading extraction.

## Main responsibilities

- Create and expose reservation settlements and invoices.
- Start Stripe Checkout sessions and process Stripe webhooks.
- Manage initial and final utility readings and OCR review.
- Publish settlement events through Kafka and send email notifications through Mailpit.

## Default port

`8086`

## Useful local URLs

- Health: [http://localhost:8086/actuator/health](http://localhost:8086/actuator/health)
- Swagger UI: [http://localhost:8086/swagger-ui.html](http://localhost:8086/swagger-ui.html)
- Gateway path: `http://localhost:8090/api/billing`

## Configuration notes

The service requires PostgreSQL credentials, `JWT_SECRET`, and the three Stripe variables from `.env`. It calls `ocr-service:8085`, uses Kafka at `kafka:9092`, and sends development mail through Mailpit.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
