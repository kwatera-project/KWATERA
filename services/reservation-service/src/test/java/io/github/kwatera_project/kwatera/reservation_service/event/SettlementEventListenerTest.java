package io.github.kwatera_project.kwatera.reservation_service.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.reservation_service.dto.SettlementStatusChangedEvent;
import io.github.kwatera_project.kwatera.reservation_service.model.SettlementStatus;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementEventListenerTest {

  @Mock private ObjectMapper objectMapper;

  @Mock private ReservationService reservationService;

  @InjectMocks private SettlementEventListener listener;

  @Test
  void shouldCallService_whenValidMessage() throws Exception {
    // given
    SettlementStatusChangedEvent event =
        new SettlementStatusChangedEvent(UUID.randomUUID(), SettlementStatus.PAID);

    byte[] message = "dummy-json".getBytes();

    when(objectMapper.readValue(message, SettlementStatusChangedEvent.class)).thenReturn(event);

    // when
    listener.handleSettlementStatusChanged(message);

    // then
    verify(reservationService)
        .handleSettlementStatusUpdate(event.reservationId(), event.settlementStatus());
  }

  @Test
  void shouldThrowRuntimeException_whenDeserializationFails() throws Exception {
    byte[] message = "bad-json".getBytes();

    when(objectMapper.readValue(message, SettlementStatusChangedEvent.class))
        .thenThrow(new RuntimeException("boom"));

    assertThrows(RuntimeException.class, () -> listener.handleSettlementStatusChanged(message));

    verifyNoInteractions(reservationService);
  }
}
