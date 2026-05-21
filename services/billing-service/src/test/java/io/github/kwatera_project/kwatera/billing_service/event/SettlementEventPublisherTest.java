package io.github.kwatera_project.kwatera.billing_service.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementStatusChangedEvent;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class SettlementEventPublisherTest {

  @Mock private KafkaTemplate<String, byte[]> kafkaTemplate;

  @Mock private ObjectMapper objectMapper;

  @InjectMocks private SettlementEventPublisher publisher;

  @Test
  void shouldPublishEventToKafka() throws Exception {
    SettlementStatusChangedEvent event =
        new SettlementStatusChangedEvent(UUID.randomUUID(), SettlementStatus.PAID);

    byte[] serialized = new byte[] {1, 2, 3};

    when(objectMapper.writeValueAsBytes(event)).thenReturn(serialized);

    publisher.publishSettlementStatusChanged(event);

    verify(kafkaTemplate).send(eq("payment-status-changed"), eq(serialized));
  }

  @Test
  void shouldThrowWhenSerializationFails() throws Exception {
    SettlementStatusChangedEvent event =
        new SettlementStatusChangedEvent(UUID.randomUUID(), SettlementStatus.PAID);

    when(objectMapper.writeValueAsBytes(event)).thenThrow(new JsonProcessingException("boom") {});

    assertThrows(RuntimeException.class, () -> publisher.publishSettlementStatusChanged(event));
  }
}
