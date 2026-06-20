package io.github.kwatera_project.kwatera.reservation_service.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OccupancyDtoTest {

  @Test
  void allArgsConstructorAndGetters() {
    UUID reservationId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.of(2026, 6, 1);
    LocalDate end = LocalDate.of(2026, 6, 7);

    OccupancyDto dto =
        new OccupancyDto(
            reservationId,
            unitId,
            "Suite 101",
            start,
            end,
            "CONFIRMED",
            "guest@example.com",
            "Guest Name");

    assertEquals(reservationId, dto.getReservationId());
    assertEquals(unitId, dto.getUnitId());
    assertEquals("Suite 101", dto.getUnitName());
    assertEquals(start, dto.getStartDate());
    assertEquals(end, dto.getEndDate());
    assertEquals("CONFIRMED", dto.getStatus());
    assertEquals("guest@example.com", dto.getGuestEmail());
    assertEquals("Guest Name", dto.getGuestName());
  }

  @Test
  void noArgsConstructorAndSetters() {
    UUID reservationId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.of(2026, 7, 1);
    LocalDate end = LocalDate.of(2026, 7, 5);

    OccupancyDto dto = new OccupancyDto();
    dto.setReservationId(reservationId);
    dto.setUnitId(unitId);
    dto.setUnitName("Penthouse");
    dto.setStartDate(start);
    dto.setEndDate(end);
    dto.setStatus("PENDING");
    dto.setGuestEmail("guest@example.com");
    dto.setGuestName("Guest Name");

    assertEquals(reservationId, dto.getReservationId());
    assertEquals(unitId, dto.getUnitId());
    assertEquals("Penthouse", dto.getUnitName());
    assertEquals(start, dto.getStartDate());
    assertEquals(end, dto.getEndDate());
    assertEquals("PENDING", dto.getStatus());
    assertEquals("guest@example.com", dto.getGuestEmail());
    assertEquals("Guest Name", dto.getGuestName());
  }
}
