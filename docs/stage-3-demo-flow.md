# Stage 3 Demo Flow

This checklist shows how to manually observe the Stage 3 deliverables in the current repository. It describes implemented UI/API behavior only.

## 1. Start the local environment

From the repository root:

```bash
docker compose -f infra/compose/docker-compose.yml up --build
```

Open the frontend:

- [http://localhost:5173](http://localhost:5173)

Useful support pages:

- Eureka: [http://localhost:8761](http://localhost:8761)
- Mailpit: [http://localhost:8025](http://localhost:8025)
- OCR health: [http://localhost:8085/health](http://localhost:8085/health)

## 2. Log in with seeded demo users

The database migrations seed demo users. Use the password `pass`.

| Role | Email |
| --- | --- |
| Admin | `admin@example.com` |
| Owner | `owner1@example.com` or `owner2@example.com` |
| Guest | `guest1@example.com` or `guest2@example.com` |

If the seeded database was replaced, create a user through the Sign Up page or call `POST /api/auth/register` through the API Gateway at `http://localhost:8090`.

## 3. Browse and create a reservation

1. Open [http://localhost:5173/catalog](http://localhost:5173/catalog).
2. Open a property from the catalog.
3. Choose stay dates in the availability calendar.
4. Select an available unit and click `Book these dates`.
5. Observe that the frontend calls the reservation API and then tries to create a billing checkout session.

Notes:

- Reservation creation is implemented for authenticated guests.
- Checkout redirection depends on valid Stripe environment variables in the local Compose environment.
- Seeded reservations are available immediately after migrations if you only need to observe reservation details.

## 4. Observe reservation administration

1. Log in as `admin@example.com` or `owner1@example.com`.
2. Open the profile menu.
3. Click `Reservations`.
4. Review the reservation list, status filter, and reservation detail links.
5. Change a reservation status from the available admin controls.
6. Open Mailpit and verify the reservation status email when the status change sends one.

The admin/owner reservation list is implemented at `/admin/reservations`.

## 5. Observe settlements and payments

1. Log in as a guest and open `My Reservations`, or log in as an admin/owner and open `Reservations`.
2. Open a reservation detail page.
3. Click `View Settlement`.
4. Review settlement status, totals, line items, currency display, and payment buttons where settlement items are present.
5. If Stripe is configured locally, click a payment button to trigger checkout.
6. If Stripe is not configured, use the seeded settlement/payment data to observe statuses in the settlement page and dashboard.

Seeded settlements and payment transactions are created by the migration files under `services/db-migrations/src/main/resources/db/migration/`.

## 6. Observe OCR water meter reading flow

1. Log in as a guest with a reservation that has a settlement.
2. Open `My Reservations`.
3. Click `Water Meter` on a reservation card when the link is shown.
4. Upload a water meter image for the check-in reading.
5. Observe the returned reading status and, when present, the confidence score.
6. After an initial reading is approved, upload a check-out reading.

The guest meter page shows reading status, reading value, confidence, and source for check-in and check-out readings.

Limitations to keep in mind:

- The OCR container expects the YOLO model file described in [services/ocr-service/README.md](../services/ocr-service/README.md). Without the model, OCR image processing may not complete successfully.
- The implemented fallback states include `REQUEST_REUPLOAD` and `REQUEST_MANUAL_REVIEW`.

## 7. Review manual OCR confirmation

1. Log in as `admin@example.com` or an owner account.
2. Open `Reservations`.
3. Click `Meter Readings` for a reservation where the link is shown.
4. Review uploaded meter photos, OCR values, confidence scores, and statuses.
5. If a reading is in `REQUEST_MANUAL_REVIEW`, enter a corrected value and approve it.

The admin/owner review page is implemented at `/admin/settlements/:settlementId/meter-readings`.

## 8. Verify generated emails

Open [http://localhost:8025](http://localhost:8025) after triggering reservation, settlement, or payment-related actions.

Implemented local email events include:

- reservation created,
- reservation status changed,
- settlement created / issued,
- payment or settlement status changed.

## 9. View dashboard output

1. Log in as `admin@example.com` or an owner account.
2. Open the profile menu.
3. Click `Dashboard`.
4. Review reservation metrics, billing metrics, settlement payment status, revenue, and unpaid balance widgets.

The dashboard uses:

- reservation metrics from `/api/v1/admin/dashboard/reservations`,
- billing metrics from `/api/v1/admin/dashboard/billing`.

## 10. View AI pricing output

1. Log in as an owner account.
2. Open the profile menu.
3. Click `Manage properties`.
4. Open `Manage Units` for a property.
5. Review the `Suggested price` value on each unit card.

The owner units page calls the AI Pricing Service through the API Gateway at `/api/predict/price/property/{propertyId}/unit/{unitId}`.

### Try AI pricing directly through Swagger

You can also test dynamic pricing directly through the AI Pricing Service Swagger UI.

1. Open AI Pricing Service Swagger UI:

    * http://localhost:8087/swagger-ui.html

2. Find the prediction endpoint:

    * `GET /api/predict/price/property/{propertyId}/unit/{unitId}`
    * `GET /api/predict/price/property/{propertyId}/unit/{unitId}/date/{date}`

3. Use a seeded or existing `propertyId` and `unitId`.

4. First execute the endpoint without `date`. This uses the current local date on the backend.

5. Then execute the endpoint with different `date` values in `YYYY-MM-DD` format, for example:

    * `2026-06-15`
    * `2026-07-15`
    * `2026-08-15`

6. Compare returned predicted prices and observe how the model output changes for different dates.

The endpoint with `/date/{date}` is useful for demo purposes because it allows checking the same property/unit pair for different stay dates without changing frontend state or database data.


## Current Stage 3 gaps visible in the repository

- Weather-related output is not exposed by the current frontend routes or backend controllers.
- Add/edit/delete buttons on owner property and unit management pages are visible UI controls, but the repository does not include implemented create/update/delete behavior for those controls.
- Payment checkout requires local Stripe configuration; otherwise use seeded payment and settlement data for observation.
