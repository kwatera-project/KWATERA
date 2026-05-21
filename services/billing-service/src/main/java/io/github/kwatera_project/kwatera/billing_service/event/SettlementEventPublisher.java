package io.github.kwatera_project.kwatera.billing_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementStatusChangedEvent;
import io.github.kwatera_project.kwatera.billing_service.exception.KafkaEventSerializationException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettlementEventPublisher {
  private final KafkaTemplate<String, byte[]> kafkaTemplate;
  private final ObjectMapper objectMapper;

  public void publishSettlementStatusChanged(SettlementStatusChangedEvent event) {
    try {
      byte[] payload = objectMapper.writeValueAsBytes(event);

      kafkaTemplate.send("payment-status-changed", payload);

    } catch (JsonProcessingException e) {
      throw new KafkaEventSerializationException("Failed to serialize Kafka event", e);
    }
  }
}
