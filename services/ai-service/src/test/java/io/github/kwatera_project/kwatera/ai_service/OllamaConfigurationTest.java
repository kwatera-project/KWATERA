package io.github.kwatera_project.kwatera.ai_service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sun.net.httpserver.HttpServer;
import io.github.kwatera_project.kwatera.ai_service.config.AiProperties;
import io.github.kwatera_project.kwatera.ai_service.config.OllamaConfiguration;
import io.github.kwatera_project.kwatera.ai_service.error.AiException;
import io.github.kwatera_project.kwatera.ai_service.service.AiInference;
import io.micrometer.observation.ObservationRegistry;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class OllamaConfigurationTest {
  static AiProperties properties() {
    return properties("http://127.0.0.1:1", false, AiProperties.TranslationFormat.JSON);
  }

  static AiProperties properties(
      String url, boolean detectionEnabled, AiProperties.TranslationFormat format) {
    return new AiProperties(
        url,
        "test-model:tag",
        "test-translation:tag",
        format,
        detectionEnabled,
        Duration.ofMillis(50),
        Duration.ofSeconds(2),
        Duration.ofMillis(50),
        0.1,
        128,
        8192,
        2048,
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
    assertThat(request.getValue().model()).isEqualTo(properties().summaryModel());
    assertThat(request.getValue().format()).isInstanceOf(java.util.Map.class);
    verifyNoMoreInteractions(api);
  }

  @Test
  void createsDistinctBoundedDiagnosticAndInferenceClients() {
    var config = new OllamaConfiguration();
    assertThat(config.inferenceOllamaApi(properties(), new JsonMapper()))
        .isNotSameAs(config.readinessOllamaApi(properties()));
  }

  @Test
  void sendsTranslationOnlyTruncationGuardAndContextOnTheWire() throws Exception {
    var mapper = new JsonMapper();
    var requests = new ConcurrentLinkedQueue<JsonNode>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/chat",
        exchange -> {
          try (exchange) {
            requests.add(mapper.readTree(exchange.getRequestBody()));
            var message = Map.of("role", "assistant", "content", "{\"summary\":\"Summary\"}");
            byte[] response =
                mapper.writeValueAsBytes(
                    Map.of(
                        "model",
                        "test-model:tag",
                        "done",
                        true,
                        "done_reason",
                        "stop",
                        "message",
                        message));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
          }
        });
    server.start();
    try {
      var properties =
          properties(
              "http://127.0.0.1:" + server.getAddress().getPort(),
              true,
              AiProperties.TranslationFormat.TRANSLATE_GEMMA);
      var config = new OllamaConfiguration();
      var api = config.inferenceOllamaApi(properties, mapper);
      var model = config.ollamaChatModel(properties, ObservationRegistry.NOOP, api);
      var inference = new AiInference(model, mapper, properties);

      inference.translateText("short");
      String longText = "日".repeat(4000);
      inference.translateText(longText);
      inference.generate("Summarize", Map.of("reviews", "hello"), "summary");

      var shortTranslation = requests.remove();
      assertThat(shortTranslation.get("options").get("num_ctx").asInt()).isEqualTo(2048);
      assertThat(shortTranslation.get("truncate").asBoolean()).isFalse();
      assertThat(shortTranslation.has("keep_alive")).isFalse();
      var longTranslation = requests.remove();
      assertThat(longTranslation.get("messages").get(0).get("content").asString())
          .isEqualTo(longText);
      assertThat(longTranslation.get("options").get("num_ctx").asInt()).isGreaterThan(12000);
      assertThat(longTranslation.get("truncate").asBoolean()).isFalse();
      var summary = requests.remove();
      assertThat(summary.get("options").get("num_ctx").asInt()).isEqualTo(8192);
      assertThat(summary.has("truncate")).isFalse();
    } finally {
      server.stop(0);
    }
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
                "kwatera.ai.summary-model=test",
                "kwatera.ai.translation-model=test",
                "kwatera.ai.translation-format=JSON",
                "kwatera.ai.connect-timeout=1s",
                "kwatera.ai.request-timeout=1s",
                "kwatera.ai.readiness-timeout=1s",
                "kwatera.ai.max-output-tokens=10",
                "kwatera.ai.context-tokens=8192",
                "kwatera.ai.translation-context-tokens=4096",
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
    runner
        .withPropertyValues(
            "kwatera.security.internal-token=valid-private-internal-token-with-32-characters",
            "kwatera.ai.translation-context-tokens=0")
        .run(context -> assertThat(context).hasFailed());
    runner
        .withPropertyValues(
            "kwatera.security.internal-token=valid-private-internal-token-with-32-characters",
            "kwatera.ai.language-detection-enabled=invalid")
        .run(context -> assertThat(context).hasFailed());
  }
}
