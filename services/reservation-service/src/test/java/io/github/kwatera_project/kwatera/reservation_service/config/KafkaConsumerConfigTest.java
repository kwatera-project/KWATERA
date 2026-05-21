package io.github.kwatera_project.kwatera.reservation_service.config;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

class KafkaConsumerConfigTest {

  private final KafkaConsumerConfig config = new KafkaConsumerConfig();

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
}
