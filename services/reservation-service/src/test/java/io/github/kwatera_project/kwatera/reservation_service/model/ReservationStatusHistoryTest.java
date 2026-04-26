package io.github.kwatera_project.kwatera.reservation_service.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservationStatusHistoryTest {

  @Test
  void shouldSetAndGetAllFields() {
    ReservationStatusHistory history = new ReservationStatusHistory();
    UUID id = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    UUID changedBy = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    history.setId(id);
    history.setReservationId(reservationId);
    history.setOldStatus(ReservationStatus.PENDING);
    history.setNewStatus(ReservationStatus.CONFIRMED);
    history.setChangedBy(changedBy);
    history.setChangedAt(now);

    assertEquals(id, history.getId());
    assertEquals(reservationId, history.getReservationId());
    assertEquals(ReservationStatus.PENDING, history.getOldStatus());
    assertEquals(ReservationStatus.CONFIRMED, history.getNewStatus());
    assertEquals(changedBy, history.getChangedBy());
    assertEquals(now, history.getChangedAt());
  }
}
