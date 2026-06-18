# Integration Tests

This document describes the integration tests in the KWATERA project, including what each test covers, how the Spring context is set up, and how to run the tests locally.

---

## 1. Billing-Service Integration Test

**File**:
`services/billing-service/src/test/java/io/github/kwatera_project/kwatera/billing_service/integration/BillingIntegrationTest.java`

### Spring Context

The test uses `@SpringBootTest` without `webEnvironment`, which loads the full application context without starting an HTTP server. Service beans and repositories are autowired directly into the test class.

### Database

A **real PostgreSQL database** is provided by **Testcontainers** (`@Testcontainers`, `@Container`). A `postgres:16-alpine` container is started once for the test class and its JDBC URL, username, and password are registered dynamically via `@DynamicPropertySource`. The schema is managed by `spring.jpa.hibernate.ddl-auto=create-drop`.

### What the Test Covers

The test exercises the full settlement and media-reading lifecycle without mocking the service or repository layers:

| Scenario | Description |
|---|---|
| **Create settlement** | `SettlementService.createSettlement(...)` persists a settlement with correct amounts |
| **Persist to PostgreSQL** | Settlement is readable from `SettlementRepository` after creation |
| **Add utility charge** | `addUtilitySettlementItem(...)` appends a water charge and recalculates totals |
| **Register payment** | `registerPayment(...)` updates `amountPaid` and `balanceDue` |
| **Block duplicate utility charge** | A second water charge for the same unit throws `IllegalStateException` |
| **OCR auto-approved reading** | A high-confidence OCR result triggers a water charge automatically |
| **OCR invalid value** | An unparseable OCR reading sets status `REQUEST_REUPLOAD`, no charge added |
| **OCR low-confidence reading** | A below-threshold confidence score sets status `REQUEST_REUPLOAD`, no charge added |

### External Dependencies Mocked

| Bean | Why mocked |
|---|---|
| `OcrClient` | Avoids calls to an external OCR HTTP service |
| `PropertyClient` | Avoids calls to the property-service |
| `ReservationClient` | Avoids calls to the reservation-service |
| `SettlementEventPublisher` | Avoids Kafka event publishing |
| `EmailNotificationService` | Avoids SMTP mail sending |

---

## 2. Reservation-Service Integration Test

**File**:
`services/reservation-service/src/test/java/io/github/kwatera_project/kwatera/reservation_service/integration/ReservationIntegrationTest.java`

### Spring Context

The test uses `@SpringBootTest(webEnvironment = MOCK)` together with `@AutoConfigureMockMvc`. This loads the **full** application context (real `ReservationService`, real `ReservationRepository`, real JPA with H2) and exposes the Spring MVC layer through `MockMvc`. Requests pass through the complete security filter chain, which means authentication must be provided explicitly.

### Database

An **in-memory H2 database** is used. The base configuration lives in `src/test/resources/application.yaml` (already present in the repository) and sets:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Hibernate creates and drops the schema automatically. No Docker or external database is required.

### What the Test Covers

The test exercises the full HTTP → Controller → Service → Repository path:

| Scenario | Description |
|---|---|
| **Create reservation** | `POST /api/v1/reservations` as `ROLE_GUEST` returns `201 Created` |
| **Persistence check** | Exactly one `Reservation` row exists in `ReservationRepository` after the request |
| **Field assertions** | `unitId`, `userId`, `startDate`, `endDate`, `status = PENDING`, `pricePerNightSnapshot`, `totalPrice = pricePerNight × nights` all match expected values |
| **Overlap rejection** | A second `POST` for the same unit with an overlapping date range returns `409 Conflict` |
| **No duplicate persisted** | After the conflicting request the repository still contains exactly one reservation |

### External Dependencies Mocked

| Bean | Why mocked |
|---|---|
| `RestOperations` | Replaces the load-balanced `RestTemplate` used by `ReservationService` to fetch unit price from property-service |
| `NbpExchangeRateClient` | Avoids outbound HTTP calls to the NBP exchange-rate API |
| `EmailNotificationService` | Avoids SMTP side effects |
| `SystemEventService` | Avoids audit-event persistence (keeps assertions focused on the reservation row) |
| `JavaMailSender` | Prevents Spring Mail from requiring a real SMTP server at context startup |
| `BusinessDateProvider` | Returns a fixed deterministic date so reservation dates are always in the future |

### Authentication

Authentication is injected via `SecurityMockMvcRequestPostProcessors.authentication(...)`, setting up a `UsernamePasswordAuthenticationToken` with `ROLE_GUEST` and the user-id stored in the `details` field — the same convention used by the production `JwtAuthFilter`.

---

## 3. Running the Tests Locally

### Reservation-Service Tests

```bash
# Run all tests in reservation-service (unit + integration)
mvn -pl services/reservation-service test
```

No Docker is required. H2 is started in-memory automatically.

### Billing-Service Tests

```bash
# Run all tests in billing-service (unit + integration)
mvn -pl services/billing-service test
```

> [!IMPORTANT]
> The billing-service integration test **requires Docker** because it uses Testcontainers to start a PostgreSQL container. Make sure Docker Desktop (or another OCI-compatible runtime) is running before executing this command.

### All Services

```bash
# Run all tests across the entire multi-module project
mvn test
```

> [!NOTE]
> Running all modules will also require Docker due to the billing-service Testcontainers dependency.
