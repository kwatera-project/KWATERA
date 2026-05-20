package io.github.kwatera_project.kwatera.reservation_service.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.dto.AvailabilityDto;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AvailabilityControllerTest {

  @Test
  void shouldCallService() {

    ReservationService service = mock(ReservationService.class);

    when(service.checkAvailability(any(), any(), any()))
        .thenReturn(new AvailabilityDto(true, "ok"));

    AvailabilityController controller = new AvailabilityController(service);

    AvailabilityDto result =
        controller.checkAvailability(
            UUID.randomUUID(), LocalDate.now(), LocalDate.now().plusDays(2));

    assertTrue(result.isAvailable());
    assertEquals("ok", result.getMessage());
  }

  @Test
  void shouldReturnOccupiedDates() {
    ReservationService service = mock(ReservationService.class);
    java.util.List<io.github.kwatera_project.kwatera.reservation_service.dto.DateRangeDto> dates =
        java.util.List.of(
            new io.github.kwatera_project.kwatera.reservation_service.dto.DateRangeDto(
                LocalDate.now(), LocalDate.now().plusDays(2)));
    when(service.getOccupiedDates(any())).thenReturn(dates);

    AvailabilityController controller = new AvailabilityController(service);

    java.util.List<io.github.kwatera_project.kwatera.reservation_service.dto.DateRangeDto> result =
        controller.getOccupiedDates(UUID.randomUUID());

    assertEquals(1, result.size());
    assertEquals(dates.get(0).getStartDate(), result.get(0).getStartDate());
  }
}
