package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.dto.AvailabilityDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
    LocalDate to = from; // ten sam dzień

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
}
