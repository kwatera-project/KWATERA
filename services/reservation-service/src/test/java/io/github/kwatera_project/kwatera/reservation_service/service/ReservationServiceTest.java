package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

  @Test
  void shouldReturnAvailableWhenNoReservations() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
    ReservationService service = new ReservationService(null, null, null);

    UUID id = UUID.randomUUID();
    LocalDate from = LocalDate.now().plusDays(5);
    LocalDate to = from;

    assertThrows(ResponseStatusException.class, () -> service.checkAvailability(id, from, to));
  }

  @Test
  void shouldIgnoreCompletedReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
  void shouldCreateReservationSuccessfullyWhenDatesAreAvailable() {
    ReservationRepository repository = mock(ReservationRepository.class);
    RestTemplate restTemplate = mock(RestTemplate.class);
    ReservationService service =
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(repository, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
    ReservationService service =
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

    UUID reservationId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setStatus(ReservationStatus.PENDING);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    service.handleSettlementStatusUpdate(reservationId, SettlementStatus.ISSUED);

    verify(repository, never()).save(any());
  }

  @Test
  void shouldSetCancelledStatus_whenSettlementCancelled() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service =
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(repo, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(repo, restTemplate, mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
    ReservationService service = new ReservationService(repository, restTemplate, nbpClient);

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
        new ReservationService(
            repository, mock(RestTemplate.class), mock(NbpExchangeRateClient.class));

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
    ReservationService service = new ReservationService(repository, restTemplate, nbpClient);

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
    ReservationService service = new ReservationService(repository, restTemplate, nbpClient);

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
    ReservationService service = new ReservationService(repository, restTemplate, nbpClient);

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
}
