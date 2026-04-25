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
}
