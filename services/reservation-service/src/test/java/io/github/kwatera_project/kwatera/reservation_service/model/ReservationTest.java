package io.github.kwatera_project.kwatera.reservation_service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservationTest {

  @Test
  void shouldSetAndGetAllFields() {
    Reservation reservation = new Reservation();
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now().plusDays(5);
    Instant now = Instant.now();

    reservation.setId(id);
    reservation.setUserId(userId);
    reservation.setUnitId(unitId);
    reservation.setStartDate(start);
    reservation.setEndDate(end);
    reservation.setStatus(ReservationStatus.CONFIRMED);
    reservation.setCreatedAt(now);
    reservation.setUpdatedAt(now);

    assertEquals(id, reservation.getId());
    assertEquals(userId, reservation.getUserId());
    assertEquals(unitId, reservation.getUnitId());
    assertEquals(start, reservation.getStartDate());
    assertEquals(end, reservation.getEndDate());
    assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    assertEquals(now, reservation.getCreatedAt());
    assertEquals(now, reservation.getUpdatedAt());
  }

  @Test
  void shouldWorkWithNoArgsConstructor() {
    Reservation reservation = new Reservation();
    assertNotNull(reservation);
  }
}
