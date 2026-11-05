package io.github.kwatera_project.kwatera.ai_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kwatera_project.kwatera.ai_service.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "kwatera.security.internal-token=test-internal-token-with-at-least-32-characters",
      "kwatera.ai.base-url=http://127.0.0.1:1"
    })
class AiServiceApplicationTest {
  @Autowired OllamaChatModel model;
  @Autowired AiProperties properties;

  @Test
  void startsRealProductionConfigurationWithoutOllamaAndLoadsExternalSettings() {
    assertThat(model).isNotNull();
    assertThat(properties.baseUrl()).isEqualTo("http://127.0.0.1:1");
    assertThat(properties.requestTimeout()).isEqualTo(java.time.Duration.ofSeconds(120));
    assertThat(model.getDefaultOptions().getModel()).isEqualTo(properties.summaryModel());
  }
}
