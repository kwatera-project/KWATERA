package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.dto.AvailabilityDto;
import io.github.kwatera_project.kwatera.reservation_service.dto.CreateReservationRequest;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

  @Mock private ReservationRepository reservationRepository;

  @InjectMocks private ReservationService reservationService;

  @Test
  void shouldReturnAvailableWhenNoReservations() {
    UUID unitId = UUID.randomUUID();
    when(reservationRepository.findByUnitId(unitId)).thenReturn(List.of());

    LocalDate from = LocalDate.now().plusDays(1);
    LocalDate to = LocalDate.now().plusDays(5);
    AvailabilityDto result = reservationService.checkAvailability(unitId, from, to);

    assertTrue(result.isAvailable());
    assertEquals("Unit is available", result.getMessage());
  }

  @Test
  void shouldReturnFalseWhenDatesOverlap() {
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    Reservation reservation = new Reservation();
    reservation.setUnitId(unitId);
    reservation.setStartDate(start);
    reservation.setEndDate(end);
    reservation.setStatus(ReservationStatus.CONFIRMED);

    when(reservationRepository.findByUnitId(unitId)).thenReturn(List.of(reservation));

    AvailabilityDto result =
        reservationService.checkAvailability(unitId, start.plusDays(2), start.plusDays(4));

    assertFalse(result.isAvailable());
    assertEquals("Unit is not available in selected dates", result.getMessage());
  }

  @Test
  void shouldIgnoreCancelledReservation() {
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    Reservation reservation = new Reservation();
    reservation.setStatus(ReservationStatus.CANCELLED);
    reservation.setStartDate(start);
    reservation.setEndDate(end);

    when(reservationRepository.findByUnitId(unitId)).thenReturn(List.of(reservation));

    AvailabilityDto result =
        reservationService.checkAvailability(unitId, start.plusDays(2), start.plusDays(4));

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldThrowWhenDatesAreInvalid() {
    UUID id = UUID.randomUUID();
    LocalDate from = LocalDate.now().plusDays(5);
    LocalDate to = from;

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reservationService.checkAvailability(id, from, to));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldThrowWhenDatesAreInPast() {
    UUID id = UUID.randomUUID();
    LocalDate from = LocalDate.now().minusDays(1);
    LocalDate to = LocalDate.now().plusDays(2);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reservationService.checkAvailability(id, from, to));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldThrowWhenDatesAreNull() {
    UUID id = UUID.randomUUID();
    assertThrows(
        ResponseStatusException.class,
        () -> reservationService.checkAvailability(id, null, LocalDate.now()));
    assertThrows(
        ResponseStatusException.class,
        () -> reservationService.checkAvailability(id, LocalDate.now(), null));
  }

  @Test
  void shouldCreateReservationSuccessfully() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate from = LocalDate.now().plusDays(1);
    LocalDate to = LocalDate.now().plusDays(3);

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(from);
    request.setEndDate(to);

    when(reservationRepository.findByUnitId(unitId)).thenReturn(List.of());
    when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArguments()[0]);

    Reservation result = reservationService.createReservation(userId, request);

    assertNotNull(result);
    assertEquals(userId, result.getUserId());
    assertEquals(unitId, result.getUnitId());
    assertEquals(ReservationStatus.PENDING, result.getStatus());
    verify(reservationRepository).save(any(Reservation.class));
  }

  @Test
  void shouldThrowConflictWhenCreatingReservationOnOccupiedDates() {
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate from = LocalDate.now().plusDays(1);
    LocalDate to = LocalDate.now().plusDays(3);

    Reservation existing = new Reservation();
    existing.setStartDate(from);
    existing.setEndDate(to);
    existing.setStatus(ReservationStatus.CONFIRMED);

    when(reservationRepository.findByUnitId(unitId)).thenReturn(List.of(existing));

    CreateReservationRequest request = new CreateReservationRequest();
    request.setUnitId(unitId);
    request.setStartDate(from);
    request.setEndDate(to);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reservationService.createReservation(userId, request));
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
  }

  @Test
  void shouldIgnoreCompletedReservation() {
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(10);
    LocalDate end = LocalDate.now().plusDays(15);

    Reservation reservation = new Reservation();
    reservation.setStatus(ReservationStatus.COMPLETED);
    reservation.setStartDate(start);
    reservation.setEndDate(end);

    when(reservationRepository.findByUnitId(unitId)).thenReturn(List.of(reservation));

    AvailabilityDto result =
        reservationService.checkAvailability(unitId, start.plusDays(2), start.plusDays(4));

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldThrowWhenUnitIdIsNull() {
    LocalDate from = LocalDate.now().plusDays(1);
    LocalDate to = LocalDate.now().plusDays(3);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reservationService.checkAvailability(null, from, to));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldThrowWhenUserIdIsNull() {
    CreateReservationRequest request = new CreateReservationRequest();
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reservationService.createReservation(null, request));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldThrowWhenRequestIsNull() {
    UUID userId = UUID.randomUUID();
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> reservationService.createReservation(userId, null));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }
}
