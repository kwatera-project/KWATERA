package io.github.kwatera_project.kwatera.reservation_service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import org.junit.jupiter.api.Test;

class ReservationStatusUpdateRequestTest {

  @Test
  void shouldSetAndGetStatus() {
    ReservationStatusUpdateRequest request = new ReservationStatusUpdateRequest();

    request.setNewStatus(ReservationStatus.CONFIRMED);

    assertEquals(ReservationStatus.CONFIRMED, request.getNewStatus());
  }
}
