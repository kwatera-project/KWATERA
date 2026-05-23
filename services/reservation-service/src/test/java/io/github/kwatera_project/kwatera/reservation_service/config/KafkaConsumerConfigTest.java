package io.github.kwatera_project.kwatera.reservation_service.config;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.reservation_service.dto.SettlementStatusChangedEvent;
import io.github.kwatera_project.kwatera.reservation_service.event.SettlementEventListener;
import io.github.kwatera_project.kwatera.reservation_service.exception.KafkaDeserializationException;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerConfigTest {

  private final KafkaConsumerConfig config = new KafkaConsumerConfig();

  @Mock private ReservationService reservationService;

  @Mock private ObjectMapper objectMapper;

  @InjectMocks private SettlementEventListener listener;

  @Test
  void shouldCreateConsumerFactory() {
    ConsumerFactory<String, byte[]> factory = config.consumerFactory();

    assertNotNull(factory);
  }

  @Test
  void shouldCreateKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
        config.kafkaListenerContainerFactory();

    assertNotNull(factory);
    assertNotNull(factory.getConsumerFactory());
  }

  @Test
  void shouldThrowKafkaDeserializationException_whenMessageCannotBeDeserialized() throws Exception {

    byte[] message = "invalid-json".getBytes();

    when(objectMapper.readValue(message, SettlementStatusChangedEvent.class))
        .thenThrow(new IOException("Parsing error"));

    KafkaDeserializationException ex =
        assertThrows(
            KafkaDeserializationException.class,
            () -> listener.handleSettlementStatusChanged(message));

    assertEquals("Failed to deserialize Kafka event", ex.getMessage());

    assertInstanceOf(IOException.class, ex.getCause());

    verifyNoInteractions(reservationService);
  }
}
