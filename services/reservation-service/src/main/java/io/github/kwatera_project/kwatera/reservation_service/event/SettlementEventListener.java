package io.github.kwatera_project.kwatera.reservation_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.reservation_service.dto.SettlementStatusChangedEvent;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementEventListener {
  private final ReservationService reservationService;
  private final ObjectMapper objectMapper;

  @KafkaListener(topics = "payment-status-changed", groupId = "reservation-group")
  public void handleSettlementStatusChanged(byte[] message) {
    try {

      SettlementStatusChangedEvent event =
          objectMapper.readValue(message, SettlementStatusChangedEvent.class);

      reservationService.handleSettlementStatusUpdate(
          event.reservationId(), event.settlementStatus());

    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize Kafka event", e);
    }
  }
}
