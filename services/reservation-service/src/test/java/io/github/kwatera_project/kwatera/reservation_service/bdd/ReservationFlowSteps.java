package io.github.kwatera_project.kwatera.reservation_service.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventService;
import io.github.kwatera_project.kwatera.reservation_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import io.github.kwatera_project.kwatera.reservation_service.service.BusinessDateProvider;
import io.github.kwatera_project.kwatera.reservation_service.service.EmailNotificationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestOperations;

public class ReservationFlowSteps {

  @Autowired private MockMvc mockMvc;
  @Autowired private ReservationRepository reservationRepository;
  @Autowired private RestOperations restOperations;
  @Autowired private BusinessDateProvider businessDateProvider;
  @Autowired private EmailNotificationService emailNotificationService;
  @Autowired private SystemEventService systemEventService;

  // ── Deterministic test fixtures ──────────────────────────────────────────────

  private static final UUID UNIT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final BigDecimal PRICE_PER_NIGHT = new BigDecimal("200.00");
  // Fixed "today" – all reservation dates will be strictly after this.
  private static final LocalDate FIXED_TODAY = LocalDate.of(2027, 1, 1);
  private static final LocalDate START_DATE = LocalDate.of(2027, 6, 10);
  private static final LocalDate END_DATE = LocalDate.of(2027, 6, 15); // 5 billable nights

  // ── Clean state before each scenario ─────────────────────────────────────────

  // Reset the H2 database and reconfigure mocks before every scenario.
  @Before
  public void setUp() {
    reservationRepository.deleteAll();

    // Fix today so that START_DATE is always in the future relative to the service check.
    when(businessDateProvider.today()).thenReturn(FIXED_TODAY);

    // Stub the property-service REST call that ReservationService.fetchUnitPrice() makes.
    UnitDto unitDto = new UnitDto();
    unitDto.setPricePerNight(PRICE_PER_NIGHT);
    ResponseEntity<UnitDto> unitResponse = ResponseEntity.ok(unitDto);
    when(restOperations.exchange(
            anyString(), eq(HttpMethod.GET), any(), eq(UnitDto.class), any(UUID.class)))
        .thenReturn(unitResponse);

    // Stub email notifications and audit events to be no-ops.
    doNothing().when(emailNotificationService).sendReservationCreated(any(), anyString());
    doNothing().when(emailNotificationService).sendOwnerReservationCreated(any());
    doNothing().when(systemEventService).logSafely(any(), any(), anyString(), any(), anyString());
  }

  // ── Scenario 1: Successful reservation creation ───────────────────────────────

  // Precondition: unit is available (no existing reservations in H2).
  @Given("a unit is available for a selected future date range")
  public void aUnitIsAvailableForASelectedFutureDateRange() {
    assertThat(reservationRepository.findAll()).isEmpty();
  }

  // Post the reservation request via MockMvc (full HTTP → Controller → Service → Repository path).
  @When("the guest creates a reservation for that date range")
  public void theGuestCreatesAReservationForThatDateRange() throws Exception {
    String json = reservationJson(UNIT_ID, START_DATE, END_DATE);

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildGuestAuth(USER_ID))))
        .andExpect(status().isCreated());
  }

  // Assert HTTP 201 was returned (already verified in the When step) and that
  // a single reservation with PENDING status was persisted.
  @Then("the reservation should be created with PENDING status")
  public void theReservationShouldBeCreatedWithPendingStatus() {
    List<Reservation> all = reservationRepository.findAll();
    assertThat(all).hasSize(1);
    assertThat(all.get(0).getStatus()).isEqualTo(ReservationStatus.PENDING);
  }

  // Full persistence assertion: unit, user, dates, price snapshot, and total price.
  @And(
      "the reservation should be persisted with the correct unit, guest, dates, price snapshot, and"
          + " total price")
  public void theReservationShouldBePersistedWithCorrectFields() {
    Reservation saved = reservationRepository.findAll().get(0);

    assertThat(saved.getUnitId()).isEqualTo(UNIT_ID);
    assertThat(saved.getUserId()).isEqualTo(USER_ID);
    assertThat(saved.getStartDate()).isEqualTo(START_DATE);
    assertThat(saved.getEndDate()).isEqualTo(END_DATE);

    // Price snapshot must come from the mocked property-service response.
    assertThat(saved.getPricePerNightSnapshot()).isEqualByComparingTo(PRICE_PER_NIGHT);

    // Total price = pricePerNight * billable nights (5 nights: June 10 – June 15).
    long expectedNights = ChronoUnit.DAYS.between(START_DATE, END_DATE);
    BigDecimal expectedTotal = PRICE_PER_NIGHT.multiply(BigDecimal.valueOf(expectedNights));
    assertThat(saved.getTotalPrice()).isEqualByComparingTo(expectedTotal);

    // Id must have been assigned by the database.
    assertThat(saved.getId()).isNotNull();
  }

  // ── Scenario 2: Overlapping reservation rejection ────────────────────────────

  // Precondition: create the first (accepted) reservation.
  @Given("a guest has already created a reservation for a unit and date range")
  public void aGuestHasAlreadyCreatedAReservationForAUnitAndDateRange() throws Exception {
    String json = reservationJson(UNIT_ID, START_DATE, END_DATE);

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildGuestAuth(USER_ID))))
        .andExpect(status().isCreated());

    assertThat(reservationRepository.findAll()).hasSize(1);
  }

  // Attempt to create a second reservation with overlapping dates.
  @When("the guest tries to create another reservation for the same unit with overlapping dates")
  public void theGuestTriesToCreateAnotherReservationWithOverlappingDates() throws Exception {
    // June 12 – June 17 overlaps with the existing June 10 – June 15 reservation.
    LocalDate overlapStart = START_DATE.plusDays(2);
    LocalDate overlapEnd = END_DATE.plusDays(2);
    String overlapJson = reservationJson(UNIT_ID, overlapStart, overlapEnd);

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(overlapJson)
                .with(authentication(buildGuestAuth(USER_ID))))
        .andExpect(status().isConflict());
  }

  // The second request must have been rejected with HTTP 409 (already asserted above).
  @Then("the second reservation should be rejected with conflict")
  public void theSecondReservationShouldBeRejectedWithConflict() {
    // HTTP 409 was already asserted in the When step via MockMvc.
    // This Then step documents the expectation explicitly for readability.
    assertThat(reservationRepository.findAll()).hasSize(1);
  }

  // Only the original reservation must remain in the database.
  @And("only the original reservation should remain persisted")
  public void onlyTheOriginalReservationShouldRemainPersisted() {
    List<Reservation> all = reservationRepository.findAll();

    assertThat(all).hasSize(1);
    assertThat(all.get(0).getUnitId()).isEqualTo(UNIT_ID);
    assertThat(all.get(0).getStartDate()).isEqualTo(START_DATE);
    assertThat(all.get(0).getEndDate()).isEqualTo(END_DATE);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  // Builds a fully authenticated principal with ROLE_GUEST.
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

  // Convenience helper: builds the JSON body for a guest reservation request.
  private String reservationJson(UUID unitId, LocalDate start, LocalDate end) {
    return String.format(
        "{\"unitId\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\"}", unitId, start, end);
  }
}
