package io.github.kwatera_project.kwatera.ai_service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.ai_service.config.AiProperties;
import io.github.kwatera_project.kwatera.ai_service.config.OllamaConfiguration;
import io.github.kwatera_project.kwatera.ai_service.error.AiException;
import io.github.kwatera_project.kwatera.ai_service.service.AiInference;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.json.JsonMapper;

class OllamaConfigurationTest {
  static AiProperties properties() {
    return new AiProperties(
        "http://127.0.0.1:1",
        "test-model:tag",
        Duration.ofMillis(50),
        Duration.ofMillis(100),
        Duration.ofMillis(50),
        0.1,
        128,
        8192,
        20,
        2000,
        6000,
        2000,
        16000);
  }

  @Test
  void buildsOfficialClientWithoutStartupNetworkOrPullAndNeverRetriesInference() {
    var api = mock(OllamaApi.class);
    var model =
        new OllamaConfiguration().ollamaChatModel(properties(), ObservationRegistry.NOOP, api);
    verifyNoInteractions(api);
    assertThat(model.getDefaultOptions().getModel()).isEqualTo("test-model:tag");
    assertThat(model.getDefaultOptions().getTemperature()).isEqualTo(0.1);
    when(api.chat(any())).thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
    var inference = new AiInference(model, JsonMapper.builder().build(), properties());
    assertThatThrownBy(
            () -> inference.generate("Summarize", java.util.Map.of("reviews", "hello"), "summary"))
        .isInstanceOfSatisfying(
            AiException.class,
            exception -> assertThat(exception.code()).isEqualTo("AI_UNAVAILABLE"));
    verify(api, times(1)).chat(any());
    var request = org.mockito.ArgumentCaptor.forClass(OllamaApi.ChatRequest.class);
    verify(api).chat(request.capture());
    assertThat(request.getValue().model()).isEqualTo(properties().model());
    assertThat(request.getValue().format()).isInstanceOf(java.util.Map.class);
    verifyNoMoreInteractions(api);
  }

  @Test
  void createsDistinctBoundedDiagnosticAndInferenceClients() {
    var config = new OllamaConfiguration();
    assertThat(config.inferenceOllamaApi(properties()))
        .isNotSameAs(config.readinessOllamaApi(properties()));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({
    AiProperties.class,
    io.github.kwatera_project.kwatera.ai_service.config.InternalSecurityProperties.class
  })
  static class PropertiesConfiguration {}

  @Test
  void rejectsMissingInternalTokenAndInvalidGenerationConfiguration() {
    var runner =
        new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                "kwatera.ai.base-url=http://127.0.0.1:1",
                "kwatera.ai.model=test",
                "kwatera.ai.connect-timeout=1s",
                "kwatera.ai.request-timeout=1s",
                "kwatera.ai.readiness-timeout=1s",
                "kwatera.ai.max-output-tokens=10",
                "kwatera.ai.context-tokens=8192",
                "kwatera.ai.max-reviews=1",
                "kwatera.ai.max-review-characters=100",
                "kwatera.ai.max-total-review-characters=100",
                "kwatera.ai.max-translation-characters=100",
                "kwatera.ai.max-response-characters=100");
    runner.run(context -> assertThat(context).hasFailed());
    runner
        .withPropertyValues(
            "kwatera.security.internal-token=valid-private-internal-token-with-32-characters")
        .run(context -> assertThat(context).hasNotFailed());
    runner
        .withPropertyValues(
            "kwatera.security.internal-token=valid-private-internal-token-with-32-characters",
            "kwatera.ai.temperature=3",
            "kwatera.ai.request-timeout=0s")
        .run(context -> assertThat(context).hasFailed());
  }
}
