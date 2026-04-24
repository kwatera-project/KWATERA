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

    AvailabilityDto result =
        service.checkAvailability(unitId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5));

    assertTrue(result.isAvailable());
    assertEquals("Unit is available", result.getMessage());
  }

  @Test
  void shouldReturnFalseWhenDatesOverlap() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

    UUID unitId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.of(2026, 5, 10));
    reservation.setEndDate(LocalDate.of(2026, 5, 15));
    reservation.setStatus(ReservationStatus.CONFIRMED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(reservation));

    AvailabilityDto result =
        service.checkAvailability(unitId, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 14));

    assertFalse(result.isAvailable());
    assertEquals("Unit is not available in selected dates", result.getMessage());
  }

  @Test
  void shouldIgnoreCancelledReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

    UUID unitId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.of(2026, 5, 10));
    reservation.setEndDate(LocalDate.of(2026, 5, 15));
    reservation.setStatus(ReservationStatus.CANCELLED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(reservation));

    AvailabilityDto result =
        service.checkAvailability(unitId, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 14));

    assertTrue(result.isAvailable());
  }

  @Test
  void shouldThrowWhenDatesAreInvalid() {
    ReservationService service = new ReservationService(null);

    UUID id = UUID.randomUUID();
    LocalDate from = LocalDate.of(2026, 5, 10);
    LocalDate to = LocalDate.of(2026, 5, 10);

    assertThrows(ResponseStatusException.class, () -> service.checkAvailability(id, from, to));
  }

  @Test
  void shouldIgnoreCompletedReservation() {
    ReservationRepository repository = mock(ReservationRepository.class);
    ReservationService service = new ReservationService(repository);

    UUID unitId = UUID.randomUUID();

    Reservation reservation = new Reservation();
    reservation.setUnitId(unitId);
    reservation.setStartDate(LocalDate.of(2026, 5, 10));
    reservation.setEndDate(LocalDate.of(2026, 5, 15));
    reservation.setStatus(ReservationStatus.COMPLETED);

    when(repository.findByUnitId(unitId)).thenReturn(List.of(reservation));

    AvailabilityDto result =
        service.checkAvailability(unitId, LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 14));

    assertTrue(result.isAvailable());
  }
}
