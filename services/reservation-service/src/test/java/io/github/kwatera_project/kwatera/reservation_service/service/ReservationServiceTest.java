package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.dto.AvailabilityDto;
import io.github.kwatera_project.kwatera.reservation_service.dto.CreateReservationRequest;
import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationDetailsDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ReservationServiceTest {

  @Test
  void shouldReturnAvailableWhenNoReservations() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

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
    ReservationService service = new ReservationService(repository);

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
    ReservationService service = new ReservationService(repository);

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
    ReservationService service = new ReservationService(null);

    UUID id = UUID.randomUUID();
    LocalDate from = LocalDate.now().plusDays(5);
    LocalDate to = from;

    assertThrows(ResponseStatusException.class, () -> service.checkAvailability(id, from, to));
  }

  @Test
  void shouldIgnoreCompletedReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

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
    ReservationService service = new ReservationService(repository);

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
    ReservationService service = new ReservationService(repository);

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(start);
    request.setEndDate(end);

    when(repository.findByUnitId(unitId)).thenReturn(List.of());
    when(repository.save(any(Reservation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Reservation created = service.createReservation(userId, request);

    assertNotNull(created);
    assertEquals(userId, created.getUserId());
    assertEquals(unitId, created.getUnitId());
    assertEquals(start, created.getStartDate());
    assertEquals(end, created.getEndDate());
    assertEquals(ReservationStatus.PENDING, created.getStatus());

    ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
    verify(repository).save(captor.capture());
    Reservation saved = captor.getValue();
    assertEquals(ReservationStatus.PENDING, saved.getStatus());
  }

  @Test
  void shouldThrowConflictWhenTryingToReserveUnavailableDates() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
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
            ResponseStatusException.class, () -> service.createReservation(userId, request));

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("The selected dates are no longer available", exception.getReason());

    verify(repository, never()).save(any());
  }

  @Test
  void shouldReturnDetailsWhenUserOwnsReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

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

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    ReservationDetailsDto dto = service.getReservationDetails(reservationId, userId, false);

    assertNotNull(dto);
    assertEquals(reservationId, dto.getId());
    assertEquals(userId, dto.getUserId());
    assertEquals(unitId, dto.getUnitId());
  }

  @Test
  void shouldThrowNotFoundWhenReservationDoesNotExist() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

    UUID reservationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(repository.findById(reservationId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.getReservationDetails(reservationId, userId, false));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void shouldThrowForbiddenWhenUserDoesNotOwnReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

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
            () -> service.getReservationDetails(reservationId, differentUserId, false));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void shouldAllowViewingOtherUserReservationWhenHasManagementAccess() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

    UUID reservationId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID managerId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setId(reservationId);
    reservation.setUserId(ownerId);

    when(repository.findById(reservationId)).thenReturn(Optional.of(reservation));

    ReservationDetailsDto dto = service.getReservationDetails(reservationId, managerId, true);

    assertNotNull(dto);
    assertEquals(reservationId, dto.getId());
    assertEquals(ownerId, dto.getUserId());
  }

  @Test
  void shouldReturnAvailable_whenReservationEndsExactlyAtRequestedStart() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

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
    ReservationService service = new ReservationService(repository);

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
    ReservationService service = new ReservationService(repository);

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
    ReservationService service = new ReservationService(repository);

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
}
