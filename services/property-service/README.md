[← Back to the project README](../../README.md)

# Property Service

The Property Service owns the accommodation catalog used by guests and property owners. It manages properties, units, images, amenities, pricing inputs, and unit settlement configuration.

## Main responsibilities

- Serve public property and unit catalog data.
- Support owner-side property and unit management.
- Store and serve uploaded property and unit images.
- Provide property data to reservation, billing, authentication, and pricing flows.

## Default port

`8083`

## Useful local URLs

- Health: [http://localhost:8083/actuator/health](http://localhost:8083/actuator/health)
- Swagger UI: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
- Gateway paths: `http://localhost:8090/api/properties` and `/api/owner`

## Configuration notes

The service requires PostgreSQL credentials and `JWT_SECRET`. Docker Compose persists uploaded files by mounting `backend-storage/` at `/app/storage`. Property creation uses OpenStreetMap Nominatim for geocoding and therefore needs outbound network access.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
