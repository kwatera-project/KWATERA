package io.github.kwatera_project.kwatera.reservation_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventService;
import io.github.kwatera_project.kwatera.reservation_service.client.NbpExchangeRateClient;
import io.github.kwatera_project.kwatera.reservation_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.reservation_service.event.SettlementEventListener;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import io.github.kwatera_project.kwatera.reservation_service.service.BusinessDateProvider;
import io.github.kwatera_project.kwatera.reservation_service.service.EmailNotificationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestOperations;

// Full Spring context with in-memory H2 (configured via src/test/resources/application.yaml).
// External infrastructure (Eureka, Config Server, Kafka, mail, property-service, NBP API)
// is either disabled through test properties or replaced with Mockito mocks.
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      // Disable Spring Cloud service discovery and config server
      "spring.cloud.discovery.enabled=false",
      "eureka.client.enabled=false",
      "spring.cloud.config.enabled=false",
      // Point Kafka to a non-existing broker so the consumer never connects
      "spring.kafka.bootstrap-servers=localhost:19999",
      // Provide a dummy JWT secret so JwtService can initialise
      "jwt.secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=",
      // Mail configuration – JavaMailSender bean is mocked, but properties must resolve
      "spring.mail.host=localhost",
      "spring.mail.port=1025",
      // Business zone used by BusinessDateProvider (mocked, but the property must parse)
      "kwatera.business-zone=Europe/Warsaw",
      "kwatera.mail.from=no-reply@kwatera.test",
      "kwatera.mail.test-recipient=test@kwatera.test",
      // Keep DDL in sync with the entity model for the H2 test database
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.show-sql=false",
      // Disable mail health indicator – the actuator requires real mail beans, but we mock them
      "management.health.mail.enabled=false",
      // Prevent Kafka listener containers from auto-starting at context startup.
      // The ConsumerFactory and ContainerFactory beans are mocked below, so no real
      // broker connection is attempted.
      "spring.kafka.listener.auto-startup=false"
    })
@AutoConfigureMockMvc
class ReservationIntegrationTest {

  // ── MockMvc wired with the full security filter chain ────────────────────────

  @Autowired private MockMvc mockMvc;

  // ── Real beans under test ────────────────────────────────────────────────────

  @Autowired private ReservationRepository reservationRepository;

  // ── External dependencies replaced with mocks ───────────────────────────────

  // RestOperations is the interface implemented by the @LoadBalanced RestTemplate
  // that ReservationService uses for property-service calls.
  @MockitoBean private RestOperations restOperations;

  // Prevents outbound calls to the NBP exchange-rate API.
  @MockitoBean private NbpExchangeRateClient nbpExchangeRateClient;

  // Prevents actual SMTP/mail-sender calls.
  @MockitoBean private EmailNotificationService emailNotificationService;

  // Prevents audit event persistence from interfering with test assertions.
  @MockitoBean private SystemEventService systemEventService;

  // Satisfies JavaMailSender autowiring inside Spring Mail autoconfiguration,
  // keeping the context load clean even without a real mail server.
  @MockitoBean private JavaMailSender javaMailSender;

  // Fixes the "today" date so test dates are always deterministic and in the future.
  @MockitoBean private BusinessDateProvider businessDateProvider;

  // Mocking the Kafka ConsumerFactory and listener container factory prevents
  // KafkaConsumerConfig from attempting a real TCP connection to kafka:9092
  // (which is only available inside Docker and would fail in CI/local test runs).
  @MockitoBean private ConsumerFactory<String, byte[]> consumerFactory;

  @MockitoBean
  private ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory;

  // Mocking SettlementEventListener replaces the @KafkaListener-annotated bean
  // so that its listener is never registered with the Kafka infrastructure.
  @MockitoBean private SettlementEventListener settlementEventListener;

  // ── Test fixtures ────────────────────────────────────────────────────────────

  private static final UUID UNIT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final BigDecimal PRICE_PER_NIGHT = new BigDecimal("200.00");
  // Fixed "today" – all reservation dates will be strictly after this.
  private static final LocalDate FIXED_TODAY = LocalDate.of(2027, 1, 1);
  private static final LocalDate START_DATE = LocalDate.of(2027, 6, 10);
  private static final LocalDate END_DATE = LocalDate.of(2027, 6, 15); // 5 billable nights

  @BeforeEach
  void setUp() {
    // Clean the H2 database before each test so tests are independent.
    reservationRepository.deleteAll();

    // Fix today so that START_DATE is always in the future relative to the service check.
    when(businessDateProvider.today()).thenReturn(FIXED_TODAY);

    // Stub the property-service REST call that ReservationService.fetchUnitPrice() makes.
    // The service uses RestOperations.exchange(url, GET, entity, UnitDto.class, unitId).
    UnitDto unitDto = new UnitDto();
    unitDto.setPricePerNight(PRICE_PER_NIGHT);
    ResponseEntity<UnitDto> unitResponse = ResponseEntity.ok(unitDto);
    when(restOperations.exchange(
            anyString(), eq(HttpMethod.GET), any(), eq(UnitDto.class), any(UUID.class)))
        .thenReturn(unitResponse);

    // Stub email notifications to be no-ops (the mock records calls but does nothing).
    doNothing().when(emailNotificationService).sendReservationCreated(any(), anyString());
    doNothing().when(emailNotificationService).sendOwnerReservationCreated(any());

    // Stub system event logging to be a no-op.
    doNothing().when(systemEventService).logSafely(any(), any(), anyString(), any(), anyString());
  }

  // Builds a fully authenticated principal with the given roles.
  // The user-id is stored in the Authentication's details field,
  // mirroring how JwtAuthFilter populates it in production.
  private Authentication buildGuestAuth(UUID userId) {
    List<GrantedAuthority> authorities =
        Arrays.stream(new String[] {"ROLE_GUEST"})
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("guest@kwatera.test", null, authorities);
    auth.setDetails(userId.toString());
    return auth;
  }

  // Convenience helper: builds the JSON body for a valid guest reservation request.
  private String reservationJson(UUID unitId, LocalDate start, LocalDate end) {
    return String.format(
        "{\"unitId\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\"}", unitId, start, end);
  }

  // Verifies that a guest can create a reservation via the HTTP layer and that the
  // created entity is correctly persisted in the H2 database.
  @Test
  void shouldCreateReservationAndPersistIt() throws Exception {
    String json = reservationJson(UNIT_ID, START_DATE, END_DATE);

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildGuestAuth(USER_ID))))
        .andExpect(status().isCreated());

    // Assert exactly one reservation was persisted.
    List<Reservation> all = reservationRepository.findAll();
    assertThat(all).hasSize(1);

    Reservation saved = all.get(0);

    // Assert core fields match the request payload.
    assertThat(saved.getUnitId()).isEqualTo(UNIT_ID);
    assertThat(saved.getUserId()).isEqualTo(USER_ID);
    assertThat(saved.getStartDate()).isEqualTo(START_DATE);
    assertThat(saved.getEndDate()).isEqualTo(END_DATE);

    // A guest reservation must be created in PENDING status.
    assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);

    // Price snapshot must come from the mocked property-service response.
    assertThat(saved.getPricePerNightSnapshot()).isEqualByComparingTo(PRICE_PER_NIGHT);

    // Total price = pricePerNight * billable nights (5 nights: June 10 – June 15).
    long expectedNights = java.time.temporal.ChronoUnit.DAYS.between(START_DATE, END_DATE);
    BigDecimal expectedTotal = PRICE_PER_NIGHT.multiply(BigDecimal.valueOf(expectedNights));
    assertThat(saved.getTotalPrice()).isEqualByComparingTo(expectedTotal);

    // Id must have been assigned by the database.
    assertThat(saved.getId()).isNotNull();
  }

  // Verifies that a second reservation for the same unit with an overlapping date range
  // is rejected with HTTP 409 Conflict and that no additional reservation is persisted.
  @Test
  void shouldRejectOverlappingReservationWithConflict() throws Exception {
    String json = reservationJson(UNIT_ID, START_DATE, END_DATE);

    // First reservation – must succeed.
    mockMvc
        .perform(
            post("/api/v1/reservations")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildGuestAuth(USER_ID))))
        .andExpect(status().isCreated());

    assertThat(reservationRepository.findAll()).hasSize(1);

    // Second reservation for the same unit and overlapping dates – must be rejected.
    LocalDate overlapStart = START_DATE.plusDays(2); // June 12 – overlaps with June 10–15
    LocalDate overlapEnd = END_DATE.plusDays(2); // June 17
    String overlapJson = reservationJson(UNIT_ID, overlapStart, overlapEnd);

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(overlapJson)
                .with(authentication(buildGuestAuth(USER_ID))))
        .andExpect(status().isConflict());

    // Repository must still contain exactly the first reservation only.
    assertThat(reservationRepository.findAll()).hasSize(1);
  }
}
