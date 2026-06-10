package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventService;
import io.github.kwatera_project.kwatera.reservation_service.audit.SystemEventType;
import io.github.kwatera_project.kwatera.reservation_service.client.NbpExchangeRateClient;
import io.github.kwatera_project.kwatera.reservation_service.dto.AvailabilityDto;
import io.github.kwatera_project.kwatera.reservation_service.dto.CreateReservationRequest;
import io.github.kwatera_project.kwatera.reservation_service.dto.GuestReservationDto;
import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationDetailsDto;
import io.github.kwatera_project.kwatera.reservation_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.model.SettlementStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService.PropertyDetailsDto;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService.UnitDetailsDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

class ReservationServiceTest {

  private ReservationService reservationService(
      ReservationRepository repository,
      RestTemplate restTemplate,
      NbpExchangeRateClient nbpExchangeRateClient) {
    return new ReservationService(
        repository,
        restTemplate,
        nbpExchangeRateClient,
        mock(EmailNotificationService.class),
        new BusinessDateProvider("Europe/Warsaw"));
  }

  private ReservationService reservationService(
      ReservationRepository repository,
      RestTemplate restTemplate,
      NbpExchangeRateClient nbpExchangeRateClient,
      BusinessDateProvider businessDateProvider) {
    return new ReservationService(
        repository,
        restTemplate,
        nbpExchangeRateClient,
        mock(EmailNotificationService.class),
        businessDateProvider);
  }

  @Test
  void shouldReturnAvailableWhenNoReservations() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();

    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    LocalDate from = LocalDate.now().plusDays(1);
    LocalDate to = LocalDate.now().plusDays(5);
    AvailabilityDto result = service.checkAvailability(unitId, from, to);

    assertTrue(result.isAvailable());
    assertEquals("Unit is available", result.getMessage());
  }

  @Test
  void shouldReturnFalseWhenDatesOverlap() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();

    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    Reservation reservation = new Reservation();
    reservation.setUnitId(unitId);
    reservation.setStartDate(start);
    reservation.setEndDate(end);
    reservation.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(reservation));

    AvailabilityDto result =
        service.checkAvailability(unitId, start.plusDays(2), start.plusDays(4));

    assertFalse(result.isAvailable());
    assertEquals("Unit is not available in selected dates", result.getMessage());
  }

  @Test
  void shouldIgnoreCancelledReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();

    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    Reservation reservation = new Reservation();
    reservation.setUnitId(unitId);
    reservation.setStartDate(start);
    reservation.setEndDate(end);
    reservation.setStatus(ReservationStatus.CANCELLED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(reservation));

    AvailabilityDto result =
        service.checkAvailability(unitId, start.plusDays(2), start.plusDays(4));

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldThrowWhenDatesAreInvalid() {
    ReservationService service = reservationService(null, null, null);

    UUID id = UUID.randomUUID();
    LocalDate from = LocalDate.now().plusDays(5);
    LocalDate to = from.minusDays(1);

    assertThrows(ResponseStatusException.class, () -> service.checkAvailability(id, from, to));
  }

  @Test
  void shouldIgnoreCompletedReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();

    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    Reservation reservation = new Reservation();
    reservation.setUnitId(unitId);
    reservation.setStartDate(start);
    reservation.setEndDate(end);
    reservation.setStatus(ReservationStatus.COMPLETED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(reservation));

    AvailabilityDto result =
        service.checkAvailability(unitId, start.plusDays(2), start.plusDays(4));

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldAllowReservationStartingOnEndDate() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();

    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    Reservation reservation = new Reservation();
    reservation.setUnitId(unitId);
    reservation.setStartDate(start);
    reservation.setEndDate(end);
    reservation.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(reservation));
    AvailabilityDto result = service.checkAvailability(unitId, end, end.plusDays(3));

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldReturnUnavailableWhenSingleDayBlockOverlapsRequestedSingleDay() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();
    LocalDate blockedDay = LocalDate.now().plusDays(10);

    Reservation block = new Reservation();
    block.setUnitId(unitId);
    block.setStartDate(blockedDay);
    block.setEndDate(blockedDay);
    block.setStatus(ReservationStatus.BLOCKED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(block));

    AvailabilityDto result = service.checkAvailability(unitId, blockedDay, blockedDay);

    assertFalse(result.isAvailable());
    assertEquals("Unit is not available in selected dates", result.getMessage());
  }

  @Test
  void shouldCreateReservationSuccessfullyWhenDatesAreAvailable() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String mockToken = "some-jwt-token";
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(start);
    request.setEndDate(end);

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(UnitDto.class),
            any(UUID.class)))
        .thenReturn(ResponseEntity.ok(mockUnit));

    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reservation created = service.createReservation(userId, request, mockToken);

    assertNotNull(created);
    assertEquals(userId, created.getUserId());
    assertEquals(unitId, created.getUnitId());
    assertEquals(start, created.getStartDate());
    assertEquals(end, created.getEndDate());
    assertEquals(ReservationStatus.PENDING, created.getStatus());

    assertEquals(new BigDecimal("1000.00"), created.getTotalPrice());

    ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
    verify(repository).save(captor.capture());
    Reservation saved = captor.getValue();
    assertEquals(ReservationStatus.PENDING, saved.getStatus());
  }

  @Test
  void shouldThrowConflictWhenTryingToReserveUnavailableDates() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String mockToken = "some-jwt-token";
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(start);
    request.setEndDate(end);

    Reservation existing = new Reservation();
    existing.setUnitId(unitId);
    existing.setStartDate(start);
    existing.setEndDate(end);
    existing.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(existing));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createReservation(userId, request, mockToken));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("The selected dates are no longer available", exception.getReason());

    verify(repository, never()).save(any());
  }

  @Test
  void shouldReturnDetailsWhenUserOwnsReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(userId);
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(BigDecimal.valueOf(100));
    reservation.setPaymentCurrency("PLN");
    reservation.setPaymentExchangeRate(BigDecimal.ONE);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    ReservationDetailsDto dto = service.getReservationDetails(reservationId, userId, false, false);

    assertNotNull(dto);
    assertEquals(reservationId, dto.getId());
    assertEquals(userId, dto.getUserId());
    assertEquals(unitId, dto.getUnitId());
  }

  @Test
  void shouldThrowNotFoundWhenReservationDoesNotExist() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(repository.findById(reservationId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.getReservationDetails(reservationId, userId, false, false));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void shouldThrowForbiddenWhenUserDoesNotOwnReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID differentUserId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(ownerId);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.getReservationDetails(reservationId, differentUserId, false, false));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void shouldAllowViewingOtherUserReservationWhenHasManagementAccess() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(ownerId);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(BigDecimal.valueOf(100));
    reservation.setPaymentCurrency("PLN");
    reservation.setPaymentExchangeRate(BigDecimal.ONE);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    ReservationDetailsDto dto =
        service.getReservationDetails(reservationId, managerId, true, false);

    assertNotNull(dto);
    assertEquals(reservationId, dto.getId());
    assertEquals(ownerId, dto.getUserId());
  }

  @Test
  void shouldReturnAvailable_whenReservationEndsExactlyAtRequestedStart() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();
    LocalDate requestedStart = LocalDate.now().plusDays(10);
    LocalDate requestedEnd = LocalDate.now().plusDays(15);

    Reservation existing = new Reservation();
    existing.setStartDate(requestedStart.minusDays(5));
    existing.setEndDate(requestedStart); // Ends exactly when new starts
    existing.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(existing));

    AvailabilityDto result = service.checkAvailability(unitId, requestedStart, requestedEnd);

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldReturnAvailable_whenReservationStartsExactlyAtRequestedEnd() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();
    LocalDate requestedStart = LocalDate.now().plusDays(10);
    LocalDate requestedEnd = LocalDate.now().plusDays(15);

    Reservation existing = new Reservation();
    existing.setStartDate(requestedEnd); // Starts exactly when new ends
    existing.setEndDate(requestedEnd.plusDays(5));
    existing.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(existing));

    AvailabilityDto result = service.checkAvailability(unitId, requestedStart, requestedEnd);

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldReturnAvailable_whenReservationIsBeforeRequestedRange() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();
    LocalDate requestedStart = LocalDate.now().plusDays(10);
    LocalDate requestedEnd = LocalDate.now().plusDays(15);

    Reservation existing = new Reservation();
    existing.setStartDate(requestedStart.minusDays(10));
    existing.setEndDate(requestedStart.minusDays(5));
    existing.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(existing));

    AvailabilityDto result = service.checkAvailability(unitId, requestedStart, requestedEnd);

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldReturnAvailable_whenReservationIsAfterRequestedRange() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();
    LocalDate requestedStart = LocalDate.now().plusDays(10);
    LocalDate requestedEnd = LocalDate.now().plusDays(15);

    Reservation existing = new Reservation();
    existing.setStartDate(requestedEnd.plusDays(5));
    existing.setEndDate(requestedEnd.plusDays(10));
    existing.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(existing));

    AvailabilityDto result = service.checkAvailability(unitId, requestedStart, requestedEnd);

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldReturnDetailsWhenOwnerHasAccessToReservationUnit() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(guestId);
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(BigDecimal.valueOf(100));
    reservation.setPaymentCurrency("PLN");
    reservation.setPaymentExchangeRate(BigDecimal.ONE);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));
    when(restTemplate.getForObject(anyString(), eq(UUID[].class), any(UUID.class)))
        .thenReturn(new UUID[] {unitId});

    ReservationDetailsDto dto = service.getReservationDetails(reservationId, ownerId, false, true);

    assertNotNull(dto);
    assertEquals(reservationId, dto.getId());
    assertEquals(unitId, dto.getUnitId());
  }

  @Test
  void shouldThrowForbiddenWhenOwnerDoesNotHaveAccessToReservationUnit() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID reservationUnitId = UUID.randomUUID();
    UUID differentOwnerUnitId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(guestId);
    reservation.setUnitId(reservationUnitId);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));
    when(restTemplate.getForObject(anyString(), eq(UUID[].class), any(UUID.class)))
        .thenReturn(new UUID[] {differentOwnerUnitId});

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.getReservationDetails(reservationId, ownerId, false, true));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void shouldThrowForbiddenWhenOwnerAccessVerificationFails() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(guestId);
    reservation.setUnitId(UUID.randomUUID());

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));
    when(restTemplate.getForObject(anyString(), eq(UUID[].class), any(UUID.class)))
        .thenThrow(new RuntimeException("Property service unavailable"));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.getReservationDetails(reservationId, ownerId, false, true));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertTrue(exception.getReason().contains("Unable to verify ownership"));
  }

  @Test
  void shouldReturnMyReservations() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID userId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(userId);
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.CONFIRMED);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(BigDecimal.valueOf(100));
    reservation.setPaymentCurrency("PLN");
    reservation.setPaymentExchangeRate(BigDecimal.ONE);

    when(repository.findByUserId(userId)).thenReturn(List.of(reservation));

    List<GuestReservationDto> result = service.getMyReservations(userId);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(reservationId, result.get(0).id());
    assertEquals(unitId, result.get(0).unitId());
    assertEquals(ReservationStatus.CONFIRMED, result.get(0).status());
  }

  // --------------- getOccupancy tests ---------------

  @Test
  void getOccupancy_admin_returnsAllReservationsWithUnitName() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().minusDays(1);
    LocalDate end = LocalDate.now().plusDays(5);

    Reservation r = new Reservation();
    r.setId(UUID.randomUUID());
    r.setUnitId(unitId);
    r.setStartDate(LocalDate.now());
    r.setEndDate(LocalDate.now().plusDays(3));
    r.setStatus(ReservationStatus.CONFIRMED);

    when(restTemplate.getForObject(contains("/units/ids"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});
    when(repository.findByUnitIdIn(List.of(unitId))).thenReturn(List.of(r));

    UnitDto unitDto = new UnitDto();
    unitDto.setName("Penthouse");
    when(restTemplate.getForObject(contains("/units/" + unitId), eq(UnitDto.class)))
        .thenReturn(unitDto);

    List<io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto> result =
        service.getOccupancy(start, end, UUID.randomUUID(), true);

    long reservationEntries = result.stream().filter(o -> o.getReservationId() != null).count();
    assertEquals(1, reservationEntries);
    assertEquals(
        "Penthouse",
        result.stream().filter(o -> o.getReservationId() != null).findFirst().get().getUnitName());
  }

  @Test
  void getOccupancy_admin_fallsBackToDefaultNameWhenUnitServiceFails() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().minusDays(1);
    LocalDate end = LocalDate.now().plusDays(5);

    Reservation r = new Reservation();
    r.setId(UUID.randomUUID());
    r.setUnitId(unitId);
    r.setStartDate(LocalDate.now());
    r.setEndDate(LocalDate.now().plusDays(3));
    r.setStatus(ReservationStatus.CONFIRMED);

    when(restTemplate.getForObject(contains("/units/ids"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});
    when(repository.findByUnitIdIn(List.of(unitId))).thenReturn(List.of(r));
    when(restTemplate.getForObject(contains("/units/" + unitId), eq(UnitDto.class)))
        .thenThrow(new RuntimeException("service down"));

    List<io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto> result =
        service.getOccupancy(start, end, UUID.randomUUID(), true);

    long reservationEntries = result.stream().filter(o -> o.getReservationId() != null).count();
    assertEquals(1, reservationEntries);
    assertTrue(
        result.stream()
            .filter(o -> o.getReservationId() != null)
            .findFirst()
            .get()
            .getUnitName()
            .startsWith("Room "));
  }

  @Test
  void getOccupancy_owner_returnsOnlyOwnerReservations() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().minusDays(1);
    LocalDate end = LocalDate.now().plusDays(5);

    Reservation r = new Reservation();
    r.setId(UUID.randomUUID());
    r.setUnitId(unitId);
    r.setStartDate(LocalDate.now());
    r.setEndDate(LocalDate.now().plusDays(2));
    r.setStatus(ReservationStatus.PENDING);

    when(restTemplate.getForObject(contains("/units/ids/"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});
    when(repository.findByUnitIdIn(List.of(unitId))).thenReturn(List.of(r));

    UnitDto unitDto = new UnitDto();
    unitDto.setName("Studio A");
    when(restTemplate.getForObject(contains("/units/" + unitId), eq(UnitDto.class)))
        .thenReturn(unitDto);

    List<io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto> result =
        service.getOccupancy(start, end, ownerId, false);

    assertEquals(1, result.size());
    assertEquals("Studio A", result.get(0).getUnitName());
  }

  @Test
  void getOccupancy_owner_returnsEmptyWhenPropertyServiceFails() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID ownerId = UUID.randomUUID();

    when(restTemplate.getForObject(anyString(), eq(UUID[].class)))
        .thenThrow(new RuntimeException("connection refused"));

    List<io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto> result =
        service.getOccupancy(LocalDate.now(), LocalDate.now().plusDays(7), ownerId, false);

    assertTrue(result.isEmpty());
  }

  @Test
  void getOccupancy_owner_returnsEmptyWhenOwnerHasNoUnits() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID ownerId = UUID.randomUUID();

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(new UUID[] {});

    List<io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto> result =
        service.getOccupancy(LocalDate.now(), LocalDate.now().plusDays(7), ownerId, false);

    assertTrue(result.isEmpty());
  }

  @Test
  void getOccupancy_filtersOutReservationsOutsideDateRange() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(20);

    Reservation outside = new Reservation();
    outside.setId(UUID.randomUUID());
    outside.setUnitId(unitId);
    outside.setStartDate(LocalDate.now());
    outside.setEndDate(LocalDate.now().plusDays(3));
    outside.setStatus(ReservationStatus.CONFIRMED);

    when(restTemplate.getForObject(contains("/units/ids"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});
    when(repository.findByUnitIdIn(List.of(unitId))).thenReturn(List.of(outside));
    when(restTemplate.getForObject(contains("/units/" + unitId), eq(UnitDto.class)))
        .thenReturn(null);

    List<io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto> result =
        service.getOccupancy(start, end, UUID.randomUUID(), true);

    long reservationEntries = result.stream().filter(o -> o.getReservationId() != null).count();
    assertEquals(0, reservationEntries);
    assertEquals(1, result.size());
    assertEquals("FREE", result.get(0).getStatus());
  }

  @Test
  void getOccupancy_includesFreeStubForUnitWithNoReservations() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID unitWithRes = UUID.randomUUID();
    UUID unitFree = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now().plusDays(7);

    Reservation r = new Reservation();
    r.setId(UUID.randomUUID());
    r.setUnitId(unitWithRes);
    r.setStartDate(start);
    r.setEndDate(end);
    r.setStatus(ReservationStatus.CONFIRMED);

    when(restTemplate.getForObject(contains("/units/ids/"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitWithRes, unitFree});
    when(repository.findByUnitIdIn(List.of(unitWithRes, unitFree))).thenReturn(List.of(r));
    when(restTemplate.getForObject(anyString(), eq(UnitDto.class))).thenReturn(null);

    List<io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto> result =
        service.getOccupancy(start, end, UUID.randomUUID(), false);

    assertEquals(2, result.size());
    long freeCount = result.stream().filter(o -> "FREE".equals(o.getStatus())).count();
    long reservedCount = result.stream().filter(o -> o.getReservationId() != null).count();
    assertEquals(1, freeCount);
    assertEquals(1, reservedCount);
  }

  // --------------- getOccupiedDates tests ---------------

  @Test
  void getOccupiedDates_excludesCancelledAndCompleted() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID unitId = UUID.randomUUID();

    Reservation confirmed = new Reservation();
    confirmed.setStartDate(LocalDate.now().plusDays(1));
    confirmed.setEndDate(LocalDate.now().plusDays(5));
    confirmed.setStatus(ReservationStatus.CONFIRMED);

    Reservation cancelled = new Reservation();
    cancelled.setStartDate(LocalDate.now().plusDays(6));
    cancelled.setEndDate(LocalDate.now().plusDays(8));
    cancelled.setStatus(ReservationStatus.CANCELLED);

    Reservation completed = new Reservation();
    completed.setStartDate(LocalDate.now().plusDays(9));
    completed.setEndDate(LocalDate.now().plusDays(10));
    completed.setStatus(ReservationStatus.COMPLETED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(confirmed, cancelled, completed));

    List<io.github.kwatera_project.kwatera.reservation_service.dto.DateRangeDto> result =
        service.getOccupiedDates(unitId);

    assertEquals(1, result.size());
    assertEquals(confirmed.getStartDate(), result.get(0).getStartDate());
  }

  @Test
  void shouldSetStatusToCompleted_whenSettlementPaid() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setStatus(ReservationStatus.PENDING);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    service.handleSettlementStatusUpdate(reservationId, SettlementStatus.PAID);

    ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
    verify(repository).save(captor.capture());

    assertEquals(ReservationStatus.COMPLETED, captor.getValue().getStatus());
  }

  @Test
  void shouldSetStatusToConfirmed_whenSettlementPartiallyPaid() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setStatus(ReservationStatus.PENDING);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    service.handleSettlementStatusUpdate(reservationId, SettlementStatus.PARTIALLY_PAID);

    ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
    verify(repository).save(captor.capture());

    assertEquals(ReservationStatus.CONFIRMED, captor.getValue().getStatus());
  }

  @Test
  void shouldNotSaveReservation_whenSettlementIssued() {
    ReservationRepository repository = mock(ReservationRepository.class);
    EmailNotificationService emailNotificationService = mock(EmailNotificationService.class);
    ReservationService service =
        new ReservationService(
            repository,
            mock(RestTemplate.class),
            mock(NbpExchangeRateClient.class),
            emailNotificationService,
            new BusinessDateProvider("Europe/Warsaw"));

    UUID reservationId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setStatus(ReservationStatus.PENDING);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    service.handleSettlementStatusUpdate(reservationId, SettlementStatus.ISSUED);

    verify(repository, never()).save(any());
    verifyNoInteractions(emailNotificationService);
  }

  @Test
  void shouldSetCancelledStatus_whenSettlementCancelled() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    service.handleSettlementStatusUpdate(reservationId, SettlementStatus.CANCELLED);

    assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());

    verify(repository).save(reservation);
  }

  @Test
  void shouldThrowNotFound_whenUnitServiceReturnsNullBody() {
    // given
    ReservationRepository repo = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);

    ReservationService service =
        reservationService(repo, restTemplate, mock(NbpExchangeRateClient.class));

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(LocalDate.now().plusDays(1));
    request.setEndDate(LocalDate.now().plusDays(3));

    when(repo.findByUnitId(unitId)).thenReturn(List.of());

    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UnitDto.class), eq(unitId)))
        .thenReturn(ResponseEntity.ok(null));

    // when
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createReservation(userId, request, "token"));

    // then
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertEquals("Unit not found", ex.getReason());
  }

  @Test
  void shouldThrowBadGateway_whenUnitServiceFails() {
    ReservationRepository repo = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);

    ReservationService service =
        reservationService(repo, restTemplate, mock(NbpExchangeRateClient.class));

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(LocalDate.now().plusDays(1));
    request.setEndDate(LocalDate.now().plusDays(3));

    when(repo.findByUnitId(unitId)).thenReturn(List.of());

    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UnitDto.class), eq(unitId)))
        .thenThrow(new RuntimeException("service down"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createReservation(userId, request, "token"));

    assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    assertTrue(ex.getReason().contains("Cannot fetch unit price"));
  }

  @Test
  void shouldThrowBadRequest_whenUserIdIsNull() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    CreateReservationRequest request = new CreateReservationRequest();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> service.createReservation(null, request, "token"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("User id is required", ex.getReason());
  }

  @Test
  void shouldThrowBadRequest_whenRequestIsNull() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID userId = UUID.randomUUID();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> service.createReservation(userId, null, "token"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Reservation request is required", ex.getReason());
  }

  @Test
  void createReservation_shouldConvertCurrencyWhenProvided() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    NbpExchangeRateClient nbpClient = mock(NbpExchangeRateClient.class);
    ReservationService service = reservationService(repository, restTemplate, nbpClient);

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(12);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(start);
    request.setEndDate(end);
    request.setCurrency("EUR");

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(UnitDto.class),
            any(UUID.class)))
        .thenReturn(ResponseEntity.ok(mockUnit));
    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

    io.github.kwatera_project.kwatera.reservation_service.dto.NbpRateDto rateDto =
        new io.github.kwatera_project.kwatera.reservation_service.dto.NbpRateDto(
            "no", java.time.LocalDate.now(), BigDecimal.valueOf(4.0));
    io.github.kwatera_project.kwatera.reservation_service.dto.NbpResponseDto responseDto =
        new io.github.kwatera_project.kwatera.reservation_service.dto.NbpResponseDto(
            "A", "EUR", "code", List.of(rateDto));
    when(nbpClient.getEurExchangeRate()).thenReturn(responseDto);

    Reservation created = service.createReservation(userId, request, "token");

    assertEquals("EUR", created.getPaymentCurrency());
    assertEquals(BigDecimal.valueOf(4.0), created.getPaymentExchangeRate());
    assertEquals(new BigDecimal("400.00"), created.getTotalPrice());
  }

  @Test
  void getReservationDetails_shouldConvertCurrency() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(userId);
    reservation.setUnitId(UUID.randomUUID());
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(new BigDecimal("400.00"));
    reservation.setPaymentCurrency("EUR");
    reservation.setPaymentExchangeRate(BigDecimal.valueOf(4.0));

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    ReservationDetailsDto dto = service.getReservationDetails(reservationId, userId, false, false);

    assertEquals(new BigDecimal("100.00").setScale(2), dto.getConvertedTotalPrice());
    assertEquals("EUR", dto.getCurrencyInfo().displayCurrency());
  }

  @Test
  void getMyReservations_shouldConvertCurrency() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        reservationService(repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID userId = UUID.randomUUID();
    Reservation reservation = new Reservation();
    reservation.setId(UUID.randomUUID());
    reservation.setUserId(userId);
    reservation.setUnitId(UUID.randomUUID());
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(new BigDecimal("400.00"));
    reservation.setPaymentCurrency("EUR");
    reservation.setPaymentExchangeRate(BigDecimal.valueOf(4.0));

    when(repository.findByUserId(userId)).thenReturn(List.of(reservation));

    List<GuestReservationDto> result = service.getMyReservations(userId);

    assertEquals(1, result.size());
    assertEquals(new BigDecimal("100.00").setScale(2), result.get(0).convertedTotalPrice());
    assertEquals("EUR", result.get(0).currencyInfo().displayCurrency());
  }

  @Test
  void createReservation_shouldConvertUsdCurrencyWhenProvided() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    NbpExchangeRateClient nbpClient = mock(NbpExchangeRateClient.class);
    ReservationService service = reservationService(repository, restTemplate, nbpClient);

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(12);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(start);
    request.setEndDate(end);
    request.setCurrency("USD");

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(UnitDto.class),
            any(UUID.class)))
        .thenReturn(ResponseEntity.ok(mockUnit));
    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

    io.github.kwatera_project.kwatera.reservation_service.dto.NbpRateDto rateDto =
        new io.github.kwatera_project.kwatera.reservation_service.dto.NbpRateDto(
            "no", java.time.LocalDate.now(), BigDecimal.valueOf(4.0));
    io.github.kwatera_project.kwatera.reservation_service.dto.NbpResponseDto responseDto =
        new io.github.kwatera_project.kwatera.reservation_service.dto.NbpResponseDto(
            "A", "USD", "code", List.of(rateDto));

    when(nbpClient.getUsdExchangeRate()).thenReturn(responseDto);

    Reservation created = service.createReservation(userId, request, "token");

    assertEquals("USD", created.getPaymentCurrency());
    assertEquals(BigDecimal.valueOf(4.0), created.getPaymentExchangeRate());
    assertEquals(new BigDecimal("400.00"), created.getTotalPrice());
  }

  @Test
  void createReservation_shouldFallbackToPlnWhenNbpClientFails() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    NbpExchangeRateClient nbpClient = mock(NbpExchangeRateClient.class);
    ReservationService service = reservationService(repository, restTemplate, nbpClient);

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(12);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(start);
    request.setEndDate(end);
    request.setCurrency("USD");

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(UnitDto.class),
            any(UUID.class)))
        .thenReturn(ResponseEntity.ok(mockUnit));
    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
    when(nbpClient.getUsdExchangeRate()).thenThrow(new RuntimeException("NBP unavailable"));

    Reservation created = service.createReservation(userId, request, "token");

    assertEquals("PLN", created.getPaymentCurrency());
    assertEquals(BigDecimal.ONE, created.getPaymentExchangeRate());
    assertEquals(new BigDecimal("400.00"), created.getTotalPrice());
  }

  @Test
  void createReservation_shouldThrowBadRequestForUnsupportedCurrency() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    NbpExchangeRateClient nbpClient = mock(NbpExchangeRateClient.class);
    ReservationService service = reservationService(repository, restTemplate, nbpClient);

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(12);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(start);
    request.setEndDate(end);
    request.setCurrency("GBP");

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(UnitDto.class),
            any(UUID.class)))
        .thenReturn(ResponseEntity.ok(mockUnit));
    when(repository.findByUnitId(unitId)).thenReturn(List.of());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createReservation(userId, request, "token"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Unsupported currency", ex.getReason());
    verify(repository, never()).save(any());
  }

  @Test
  void shouldCreateBlockedReservationWithoutSendingNotifications() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    EmailNotificationService emailService = mock(EmailNotificationService.class);

    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            emailService,
            new BusinessDateProvider("Europe/Warsaw"));

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate blockedDay = LocalDate.now().plusDays(10);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(blockedDay);
    request.setEndDate(blockedDay);
    request.setCurrency("PLN");
    request.setStatus(ReservationStatus.BLOCKED);

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));

    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UnitDto.class), eq(unitId)))
        .thenReturn(ResponseEntity.ok(mockUnit));
    when(repository.save(any(Reservation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reservation created =
        service.createReservation(userId, "manual.guest@example.com", request, "Bearer mock-token");

    assertEquals(userId, created.getUserId());
    assertEquals("manual.guest@example.com", created.getGuestEmail());
    assertEquals(unitId, created.getUnitId());
    assertEquals(blockedDay, created.getStartDate());
    assertEquals(blockedDay, created.getEndDate());
    assertEquals(ReservationStatus.BLOCKED, created.getStatus());
    assertEquals(0, BigDecimal.ZERO.compareTo(created.getTotalPrice()));

    verify(repository).save(any(Reservation.class));
    verifyNoInteractions(emailService);
  }

  @Test
  void getDashboardReservationMetrics_shouldThrowIfStartAfterEnd() {
    ReservationService service = reservationService(null, null, null);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.getDashboardReservationMetrics(
                    LocalDate.now().plusDays(1), LocalDate.now(), UUID.randomUUID(), true));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void getDashboardReservationMetrics_shouldReturnZerosIfNoUnits() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service = reservationService(repository, restTemplate, null);

    when(restTemplate.getForObject(anyString(), eq(UUID[].class))).thenReturn(new UUID[0]);

    io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto metrics =
        service.getDashboardReservationMetrics(
            LocalDate.now(), LocalDate.now().plusDays(10), UUID.randomUUID(), false);

    assertEquals(0L, metrics.getTotalReservations());
    assertEquals(0.0, metrics.getOccupancyRate());
    assertEquals(0L, metrics.getOccupiedDays());
  }

  @Test
  void getDashboardReservationMetrics_shouldCalculateMetricsForAdmin() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service = reservationService(repository, restTemplate, null);

    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(10);

    when(restTemplate.getForObject(contains("/units/ids"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});
    when(repository.countReservationsInDateRange(start, end)).thenReturn(5L);

    Reservation r = new Reservation();
    r.setStartDate(start.plusDays(1));
    r.setEndDate(start.plusDays(3));
    when(repository.findActiveReservationsInDateRange(start, end)).thenReturn(List.of(r));

    io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto metrics =
        service.getDashboardReservationMetrics(start, end, UUID.randomUUID(), true);

    assertEquals(5L, metrics.getTotalReservations());
    assertEquals(2L, metrics.getOccupiedDays());
    assertEquals(20.0, metrics.getOccupancyRate());
  }

  @Test
  void getDashboardReservationMetrics_shouldCalculateMetricsForOwner() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service = reservationService(repository, restTemplate, null);

    UUID unitId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(10);

    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/ids/" + ownerId), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});

    when(repository.countReservationsInDateRangeForUnits(List.of(unitId), start, end))
        .thenReturn(3L);

    Reservation r = new Reservation();
    r.setStartDate(start.minusDays(5));
    r.setEndDate(start.plusDays(5));
    when(repository.findActiveReservationsInDateRangeForUnits(List.of(unitId), start, end))
        .thenReturn(List.of(r));

    io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto metrics =
        service.getDashboardReservationMetrics(start, end, ownerId, false);

    assertEquals(3L, metrics.getTotalReservations());
    assertEquals(5L, metrics.getOccupiedDays());
    assertEquals(50.0, metrics.getOccupancyRate());
  }

  @Test
  void getReservationDetails_shouldPopulatePropertyAndOwnerDetails_JohnOwner() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID ownerId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(userId);
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(BigDecimal.valueOf(100));
    reservation.setPaymentCurrency("PLN");
    reservation.setPaymentExchangeRate(BigDecimal.ONE);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    UnitDetailsDto mockUnit = new UnitDetailsDto(propertyId, "Luxury Villa");
    PropertyDetailsDto mockProperty = new PropertyDetailsDto("My Villa", "Zakopane", ownerId);

    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/" + unitId), eq(UnitDetailsDto.class)))
        .thenReturn(mockUnit);
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/" + propertyId),
            eq(PropertyDetailsDto.class)))
        .thenReturn(mockProperty);

    ReservationDetailsDto dto = service.getReservationDetails(reservationId, userId, false, false);

    assertNotNull(dto);
    assertEquals("Luxury Villa", dto.getUnitName());
    assertEquals("Zakopane", dto.getCity());
    assertEquals("John Owner", dto.getOwnerName());
    assertEquals("owner1@example.com", dto.getOwnerEmail());
  }

  @Test
  void getReservationDetails_shouldPopulatePropertyAndOwnerDetails_JaneOwner() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID ownerId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(userId);
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(BigDecimal.valueOf(100));
    reservation.setPaymentCurrency("PLN");
    reservation.setPaymentExchangeRate(BigDecimal.ONE);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    UnitDetailsDto mockUnit = new UnitDetailsDto(propertyId, "Charming Cabin");
    PropertyDetailsDto mockProperty = new PropertyDetailsDto("My Cabin", "Sopot", ownerId);

    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/" + unitId), eq(UnitDetailsDto.class)))
        .thenReturn(mockUnit);
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/" + propertyId),
            eq(PropertyDetailsDto.class)))
        .thenReturn(mockProperty);

    ReservationDetailsDto dto = service.getReservationDetails(reservationId, userId, false, false);

    assertNotNull(dto);
    assertEquals("Charming Cabin", dto.getUnitName());
    assertEquals("Sopot", dto.getCity());
    assertEquals("Jane Owner", dto.getOwnerName());
    assertEquals("owner2@example.com", dto.getOwnerEmail());
  }

  @Test
  void getReservationDetails_shouldPopulatePropertyAndOwnerDetails_GenericOwner() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(userId);
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(BigDecimal.valueOf(100));
    reservation.setPaymentCurrency("PLN");
    reservation.setPaymentExchangeRate(BigDecimal.ONE);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    UnitDetailsDto mockUnit = new UnitDetailsDto(propertyId, "Modern Apartment");
    PropertyDetailsDto mockProperty = new PropertyDetailsDto("My Apt", "Warsaw", ownerId);

    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/" + unitId), eq(UnitDetailsDto.class)))
        .thenReturn(mockUnit);
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/" + propertyId),
            eq(PropertyDetailsDto.class)))
        .thenReturn(mockProperty);

    ReservationDetailsDto dto = service.getReservationDetails(reservationId, userId, false, false);

    assertNotNull(dto);
    assertEquals("Modern Apartment", dto.getUnitName());
    assertEquals("Warsaw", dto.getCity());
    assertEquals("Owner " + ownerId.toString().substring(0, 8), dto.getOwnerName());
    assertEquals(
        "owner_" + ownerId.toString().substring(0, 8) + "@example.com", dto.getOwnerEmail());
  }

  @Test
  void getReservationDetails_shouldHandleErrorsGracefullyWhenPropertyServiceFails() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        reservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(userId);
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setCreatedAt(Instant.now());
    reservation.setTotalPrice(BigDecimal.valueOf(100));
    reservation.setPaymentCurrency("PLN");
    reservation.setPaymentExchangeRate(BigDecimal.ONE);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    when(restTemplate.getForObject(anyString(), eq(UnitDetailsDto.class)))
        .thenThrow(new RuntimeException("Service failure"));

    ReservationDetailsDto dto = service.getReservationDetails(reservationId, userId, false, false);

    assertNotNull(dto);
    assertEquals("Unknown Room", dto.getUnitName());
    assertEquals("Unknown City", dto.getCity());
    assertNull(dto.getOwnerName());
    assertNull(dto.getOwnerEmail());
  }

  @Test
  void getDashboardReservationMetrics_shouldUseDefaultDatesWhenNull() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service = reservationService(repository, restTemplate, null);

    UUID unitId = UUID.randomUUID();
    when(restTemplate.getForObject(contains("/units/ids"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});

    LocalDate start = LocalDate.now().withDayOfMonth(1);
    LocalDate end = LocalDate.now().with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());

    when(repository.countReservationsInDateRange(start, end)).thenReturn(10L);
    when(repository.findActiveReservationsInDateRange(start, end)).thenReturn(List.of());

    io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto metrics =
        service.getDashboardReservationMetrics(null, null, UUID.randomUUID(), true);

    assertEquals(10L, metrics.getTotalReservations());
    assertEquals(0L, metrics.getOccupiedDays());
  }

  @Test
  void getDashboardReservationMetrics_shouldHandleSingleDayRange() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service = reservationService(repository, restTemplate, null);

    UUID unitId = UUID.randomUUID();
    LocalDate singleDay = LocalDate.now();

    when(restTemplate.getForObject(contains("/units/ids"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});
    when(repository.countReservationsInDateRange(singleDay, singleDay)).thenReturn(1L);

    Reservation r = new Reservation();
    r.setStartDate(singleDay);
    r.setEndDate(singleDay.plusDays(1));
    when(repository.findActiveReservationsInDateRange(singleDay, singleDay)).thenReturn(List.of(r));

    io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto metrics =
        service.getDashboardReservationMetrics(singleDay, singleDay, UUID.randomUUID(), true);

    assertEquals(1L, metrics.getTotalReservations());
    assertEquals(0L, metrics.getOccupiedDays());
    assertEquals(0.0, metrics.getOccupancyRate());
  }

  @Test
  void getDashboardReservationMetrics_shouldCapOccupancyRateAt100() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service = reservationService(repository, restTemplate, null);

    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(5);

    when(restTemplate.getForObject(contains("/units/ids"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});

    Reservation r1 = new Reservation();
    r1.setStartDate(start);
    r1.setEndDate(end);

    Reservation r2 = new Reservation();
    r2.setStartDate(start);
    r2.setEndDate(end);

    when(repository.findActiveReservationsInDateRange(start, end)).thenReturn(List.of(r1, r2));

    io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto metrics =
        service.getDashboardReservationMetrics(start, end, UUID.randomUUID(), true);

    assertEquals(10L, metrics.getOccupiedDays());
    assertEquals(100.0, metrics.getOccupancyRate());
  }

  @Test
  void getDashboardReservationMetrics_shouldIgnoreReservationsWithNoOverlap() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service = reservationService(repository, restTemplate, null);

    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = start.plusDays(5);

    when(restTemplate.getForObject(contains("/units/ids"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});

    Reservation r = new Reservation();
    r.setStartDate(start.minusDays(5));
    r.setEndDate(start);

    when(repository.findActiveReservationsInDateRange(start, end)).thenReturn(List.of(r));

    io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto metrics =
        service.getDashboardReservationMetrics(start, end, UUID.randomUUID(), true);

    assertEquals(0L, metrics.getOccupiedDays());
    assertEquals(0.0, metrics.getOccupancyRate());
  }

  @Test
  void shouldRejectDateThatIsYesterdayInBusinessZoneEvenWhenUtcStillPreviousDay() {
    ReservationRepository repository = mock(ReservationRepository.class);
    BusinessDateProvider businessDateProvider = mock(BusinessDateProvider.class);
    ReservationService service =
        reservationService(
            repository,
            mock(RestTemplate.class),
            mock(NbpExchangeRateClient.class),
            businessDateProvider);

    UUID unitId = UUID.randomUUID();

    LocalDate businessTodayInWarsaw = LocalDate.of(2026, 6, 6);
    LocalDate businessYesterdayInWarsaw = businessTodayInWarsaw.minusDays(1);
    LocalDate checkoutDate = businessTodayInWarsaw.plusDays(1);

    when(businessDateProvider.today()).thenReturn(businessTodayInWarsaw);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.checkAvailability(unitId, businessYesterdayInWarsaw, checkoutDate));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Date is in the past", exception.getReason());

    verify(repository, never()).findByUnitId(unitId);
  }

  @Test
  void shouldUseBusinessMonthForDashboardMetricsWhenWarsawIsAlreadyNextMonthButUtcIsNot() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    BusinessDateProvider businessDateProvider = mock(BusinessDateProvider.class);
    ReservationService service =
        reservationService(
            repository, restTemplate, mock(NbpExchangeRateClient.class), businessDateProvider);

    UUID unitId = UUID.randomUUID();

    LocalDate expectedBusinessMonthStart = LocalDate.of(2026, 6, 1);
    LocalDate expectedBusinessMonthEnd = LocalDate.of(2026, 6, 30);

    when(businessDateProvider.today()).thenReturn(expectedBusinessMonthStart);
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/ids"), eq(UUID[].class)))
        .thenReturn(new UUID[] {unitId});
    when(repository.countReservationsInDateRange(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(0L);
    when(repository.findActiveReservationsInDateRange(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of());

    service.getDashboardReservationMetrics(null, null, null, true);

    verify(repository)
        .countReservationsInDateRange(expectedBusinessMonthStart, expectedBusinessMonthEnd);
    verify(repository)
        .findActiveReservationsInDateRange(expectedBusinessMonthStart, expectedBusinessMonthEnd);
  }

  @Test
  void shouldCancelExpiredPendingReservationsAndSendEmail() {
    ReservationRepository repository = mock(ReservationRepository.class);
    EmailNotificationService emailService = mock(EmailNotificationService.class);
    ReservationService service =
        new ReservationService(
            repository,
            mock(RestTemplate.class),
            mock(NbpExchangeRateClient.class),
            emailService,
            new BusinessDateProvider("Europe/Warsaw"));

    UUID resId = UUID.randomUUID();
    Reservation reservation = new Reservation();
    reservation.setId(resId);
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setGuestEmail("test@example.com");

    Instant threshold = Instant.now().minusSeconds(60);

    when(repository.findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, threshold))
        .thenReturn(List.of(reservation));

    service.cancelExpiredPendingReservations(threshold);

    assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    verify(repository).save(reservation);
    verify(emailService)
        .sendReservationStatusChanged(
            reservation,
            ReservationStatus.PENDING,
            ReservationStatus.CANCELLED,
            "test@example.com");
  }

  @Test
  void shouldProceedWithCancellationEvenIfEmailFails() {
    ReservationRepository repository = mock(ReservationRepository.class);
    EmailNotificationService emailService = mock(EmailNotificationService.class);
    ReservationService service =
        new ReservationService(
            repository,
            mock(RestTemplate.class),
            mock(NbpExchangeRateClient.class),
            emailService,
            new BusinessDateProvider("Europe/Warsaw"));

    UUID resId = UUID.randomUUID();
    Reservation reservation = new Reservation();
    reservation.setId(resId);
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setGuestEmail("test@example.com");

    Instant threshold = Instant.now().minusSeconds(60);

    when(repository.findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, threshold))
        .thenReturn(List.of(reservation));
    doThrow(new RuntimeException("Email service down"))
        .when(emailService)
        .sendReservationStatusChanged(any(), any(), any(), any());

    service.cancelExpiredPendingReservations(threshold);

    assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    verify(repository).save(reservation);
  }

  @Test
  void getReservationDetailsInternal_shouldReturnDtoSuccessfully() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    EmailNotificationService emailService = mock(EmailNotificationService.class);
    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            emailService,
            new BusinessDateProvider("Europe/Warsaw"));

    UUID reservationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(userId);
    reservation.setUnitId(unitId);
    LocalDate fixedDate = LocalDate.of(2026, java.time.Month.JUNE, 9);
    reservation.setStartDate(fixedDate);
    reservation.setEndDate(fixedDate.plusDays(2));
    reservation.setStatus(ReservationStatus.CONFIRMED);
    reservation.setCreatedAt(Instant.parse("2026-06-09T00:00:00Z"));
    reservation.setTotalPrice(BigDecimal.valueOf(100));
    reservation.setPaymentCurrency("USD");
    reservation.setPaymentExchangeRate(BigDecimal.valueOf(4.0));

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    UUID propertyId = UUID.randomUUID();
    when(restTemplate.getForObject(
            contains("/units/"), eq(ReservationService.UnitDetailsDto.class)))
        .thenReturn(new ReservationService.UnitDetailsDto(propertyId, "Unit Name"));
    when(restTemplate.getForObject(
            contains("/properties/"), eq(ReservationService.PropertyDetailsDto.class)))
        .thenReturn(
            new ReservationService.PropertyDetailsDto(
                "Property Title",
                "Warsaw",
                UUID.fromString("22222222-2222-2222-2222-222222222222")));

    ReservationDetailsDto dto = service.getReservationDetailsInternal(reservationId);

    assertNotNull(dto);
    assertEquals("Unit Name", dto.getUnitName());
    assertEquals("Warsaw", dto.getCity());
    assertEquals("John Owner", dto.getOwnerName());
    assertEquals("owner1@example.com", dto.getOwnerEmail());
    assertEquals(BigDecimal.valueOf(25).setScale(2), dto.getConvertedTotalPrice());
  }

  @Test
  void notifyUpcomingReservations_shouldSendUpcomingEmails() {
    ReservationRepository repository = mock(ReservationRepository.class);
    EmailNotificationService emailService = mock(EmailNotificationService.class);
    BusinessDateProvider businessDateProvider = mock(BusinessDateProvider.class);
    ReservationService service =
        new ReservationService(
            repository,
            mock(RestTemplate.class),
            mock(NbpExchangeRateClient.class),
            emailService,
            businessDateProvider);

    LocalDate today = LocalDate.of(2026, java.time.Month.JUNE, 6);
    when(businessDateProvider.today()).thenReturn(today);

    Reservation r = new Reservation();
    r.setId(UUID.randomUUID());
    r.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByStartDateAndStatus(today.plusDays(1), ReservationStatus.CONFIRMED))
        .thenReturn(List.of(r));

    service.notifyUpcomingReservations();

    verify(emailService).sendOwnerReservationUpcoming(r);
  }

  @Test
  void notifyUpcomingReservations_shouldProceedEvenIfEmailFails() {
    ReservationRepository repository = mock(ReservationRepository.class);
    EmailNotificationService emailService = mock(EmailNotificationService.class);
    BusinessDateProvider businessDateProvider = mock(BusinessDateProvider.class);
    ReservationService service =
        new ReservationService(
            repository,
            mock(RestTemplate.class),
            mock(NbpExchangeRateClient.class),
            emailService,
            businessDateProvider);

    LocalDate today = LocalDate.of(2026, java.time.Month.JUNE, 6);
    when(businessDateProvider.today()).thenReturn(today);

    Reservation r = new Reservation();
    r.setId(UUID.randomUUID());
    r.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByStartDateAndStatus(today.plusDays(1), ReservationStatus.CONFIRMED))
        .thenReturn(List.of(r));
    doThrow(new RuntimeException("Email service down"))
        .when(emailService)
        .sendOwnerReservationUpcoming(any());

    assertDoesNotThrow(service::notifyUpcomingReservations);
  }

  @Test
  void shouldCreateBlockedReservation_whenOwnerOrAdminBlocksDates() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    EmailNotificationService emailService = mock(EmailNotificationService.class);
    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            emailService,
            new BusinessDateProvider("Europe/Warsaw"));

    UUID actorId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String mockToken = "some-jwt-token";
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(start);
    request.setEndDate(end);

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));

    // Mock unit ownership check
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/ids/{ownerId}"),
            eq(UUID[].class),
            eq(actorId)))
        .thenReturn(new UUID[] {unitId});

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(UnitDto.class),
            any(UUID.class)))
        .thenReturn(ResponseEntity.ok(mockUnit));

    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reservation created = service.createReservation(actorId, false, true, request, mockToken);

    assertNotNull(created);
    assertEquals(actorId, created.getUserId());
    assertNull(created.getGuestEmail());
    assertEquals(ReservationStatus.BLOCKED, created.getStatus());
    verify(emailService, never()).sendReservationCreated(any(), any());
  }

  @Test
  void shouldCreateReservation_whenOwnerCreatesManualReservationForGuest() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    EmailNotificationService emailService = mock(EmailNotificationService.class);
    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            emailService,
            new BusinessDateProvider("Europe/Warsaw"));

    UUID ownerId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String mockToken = "some-jwt-token";
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(start);
    request.setEndDate(end);
    request.setGuestEmail("guest@test.com");

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));

    // Mock unit ownership check
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/ids/{ownerId}"),
            eq(UUID[].class),
            eq(ownerId)))
        .thenReturn(new UUID[] {unitId});

    // Mock unit price check
    when(restTemplate.exchange(
            contains("/units/"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(UnitDto.class),
            eq(unitId)))
        .thenReturn(ResponseEntity.ok(mockUnit));

    // Mock guest ID lookup
    java.util.Map<String, Object> guestMap = new java.util.HashMap<>();
    guestMap.put("id", guestId.toString());
    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(java.util.Map.class),
            eq("guest@test.com")))
        .thenReturn(ResponseEntity.ok(guestMap));

    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reservation created = service.createReservation(ownerId, false, true, request, mockToken);

    assertNotNull(created);
    assertEquals(guestId, created.getUserId());
    assertEquals("guest@test.com", created.getGuestEmail());
    assertEquals(ReservationStatus.CONFIRMED, created.getStatus());
    verify(emailService).sendReservationCreated(created, "guest@test.com");
  }

  @Test
  void shouldFailCreateReservation_whenOwnerCreatesManualReservationAndGuestNotFound() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            mock(EmailNotificationService.class),
            new BusinessDateProvider("Europe/Warsaw"));

    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String mockToken = "some-jwt-token";

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(LocalDate.now().plusDays(10));
    request.setEndDate(LocalDate.now().plusDays(15));
    request.setGuestEmail("nonexistent@test.com");

    // Mock unit ownership check
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/ids/{ownerId}"),
            eq(UUID[].class),
            eq(ownerId)))
        .thenReturn(new UUID[] {unitId});

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(java.util.Map.class),
            eq("nonexistent@test.com")))
        .thenThrow(
            new org.springframework.web.client.HttpClientErrorException(HttpStatus.NOT_FOUND));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> {
              service.createReservation(ownerId, false, true, request, mockToken);
            });

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertTrue(exception.getReason().contains("Guest with the specified email does not exist"));
  }

  @Test
  void shouldFailCreateReservation_whenOwnerReservesUnitTheyDoNotOwn() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            mock(EmailNotificationService.class),
            new BusinessDateProvider("Europe/Warsaw"));

    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String mockToken = "some-jwt-token";

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(LocalDate.now().plusDays(10));
    request.setEndDate(LocalDate.now().plusDays(15));

    // Mock unit ownership check
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/ids/{ownerId}"),
            eq(UUID[].class),
            eq(ownerId)))
        .thenReturn(new UUID[] {UUID.randomUUID()}); // return different unit ID

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> {
              service.createReservation(ownerId, false, true, request, mockToken);
            });

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertTrue(exception.getReason().contains("You do not own this unit"));
  }

  @Test
  void shouldCreateSingleDayBlockedReservation_whenOwnerBlocksDates() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    EmailNotificationService emailService = mock(EmailNotificationService.class);
    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            emailService,
            new BusinessDateProvider("Europe/Warsaw"));

    UUID actorId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String mockToken = "some-jwt-token";
    LocalDate blockDay = LocalDate.now().plusDays(10);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(blockDay);
    request.setEndDate(blockDay);

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));

    // Mock unit ownership check
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/ids/{ownerId}"),
            eq(UUID[].class),
            eq(actorId)))
        .thenReturn(new UUID[] {unitId});

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(UnitDto.class),
            any(UUID.class)))
        .thenReturn(ResponseEntity.ok(mockUnit));

    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reservation created = service.createReservation(actorId, false, true, request, mockToken);

    assertNotNull(created);
    assertEquals(actorId, created.getUserId());
    assertNull(created.getGuestEmail());
    assertEquals(ReservationStatus.BLOCKED, created.getStatus());
    assertEquals(blockDay, created.getStartDate());
    assertEquals(blockDay, created.getEndDate());
    assertEquals(0, BigDecimal.ZERO.compareTo(created.getTotalPrice()));
    verify(emailService, never()).sendReservationCreated(any(), any());
  }

  @Test
  void shouldFailCreateReservation_whenNormalReservationHasZeroBillableNights() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            mock(EmailNotificationService.class),
            new BusinessDateProvider("Europe/Warsaw"));

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String mockToken = "some-jwt-token";
    LocalDate sameDay = LocalDate.now().plusDays(10);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(sameDay);
    request.setEndDate(sameDay);
    request.setStatus(ReservationStatus.PENDING);

    when(repository.findByUnitId(unitId)).thenReturn(List.of());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createReservation(userId, request, mockToken));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertTrue(
        exception.getReason().contains("Reservation must include at least one billable night"));
  }

  @Test
  void shouldLogReservationCreatedEvent_whenGuestCreatesReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    SystemEventService systemEventService = mock(SystemEventService.class);
    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            mock(EmailNotificationService.class),
            new BusinessDateProvider("Europe/Warsaw"),
            systemEventService);

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(LocalDate.now().plusDays(10));
    request.setEndDate(LocalDate.now().plusDays(12));

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UnitDto.class), eq(unitId)))
        .thenReturn(ResponseEntity.ok(mockUnit));
    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class)))
        .thenAnswer(
            invocation -> {
              Reservation reservation = invocation.getArgument(0);
              reservation.setId(UUID.randomUUID());
              return reservation;
            });

    Reservation created = service.createReservation(userId, request, "Bearer token");

    verify(systemEventService)
        .logSafely(
            eq(SystemEventType.RESERVATION_CREATED),
            eq(userId),
            eq(SystemEventService.ENTITY_TYPE_RESERVATION),
            eq(created.getId()),
            contains("unitId=" + unitId));
  }

  @Test
  void shouldLogUnitBlockedEvent_whenOwnerBlocksDates() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    SystemEventService systemEventService = mock(SystemEventService.class);
    ReservationService service =
        new ReservationService(
            repository,
            restTemplate,
            mock(NbpExchangeRateClient.class),
            mock(EmailNotificationService.class),
            new BusinessDateProvider("Europe/Warsaw"),
            systemEventService);

    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(LocalDate.now().plusDays(10));
    request.setEndDate(LocalDate.now().plusDays(10));

    UnitDto mockUnit = new UnitDto();
    mockUnit.setPricePerNight(new BigDecimal("200.00"));
    when(restTemplate.getForObject(
            eq("http://property-service/api/properties/units/ids/{ownerId}"),
            eq(UUID[].class),
            eq(ownerId)))
        .thenReturn(new UUID[] {unitId});
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UnitDto.class), eq(unitId)))
        .thenReturn(ResponseEntity.ok(mockUnit));
    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class)))
        .thenAnswer(
            invocation -> {
              Reservation reservation = invocation.getArgument(0);
              reservation.setId(UUID.randomUUID());
              return reservation;
            });

    Reservation created = service.createReservation(ownerId, false, true, request, "Bearer token");

    verify(systemEventService)
        .logSafely(
            eq(SystemEventType.UNIT_BLOCKED),
            eq(ownerId),
            eq(SystemEventService.ENTITY_TYPE_RESERVATION),
            eq(created.getId()),
            contains("status=BLOCKED"));
  }

  @Test
  void shouldLogExpiredReservationCancelledEvent_whenCleanupCancelsPendingReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    SystemEventService systemEventService = mock(SystemEventService.class);
    ReservationService service =
        new ReservationService(
            repository,
            mock(RestTemplate.class),
            mock(NbpExchangeRateClient.class),
            mock(EmailNotificationService.class),
            new BusinessDateProvider("Europe/Warsaw"),
            systemEventService);

    UUID reservationId = UUID.randomUUID();
    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUnitId(UUID.randomUUID());
    reservation.setStartDate(LocalDate.now().plusDays(1));
    reservation.setEndDate(LocalDate.now().plusDays(2));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setGuestEmail("guest@test.com");
    Instant threshold = Instant.now().minusSeconds(60);

    when(repository.findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, threshold))
        .thenReturn(List.of(reservation));

    service.cancelExpiredPendingReservations(threshold);

    verify(systemEventService)
        .logSafely(
            eq(SystemEventType.EXPIRED_RESERVATION_CANCELLED),
            isNull(),
            eq(SystemEventService.ENTITY_TYPE_RESERVATION),
            eq(reservationId),
            contains("newStatus=CANCELLED"));
  }
}
