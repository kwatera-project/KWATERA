package io.github.kwatera_project.kwatera.reservation_service.conversation.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.reservation_service.config.TranslationClientConfig;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.server.ResponseStatusException;

class MessageTranslationClientTest {
  private MockRestServiceServer server;
  private MessageTranslationClient client;

  @BeforeEach
  void setup() {
    var builder =
        new TranslationClientConfig()
            .translationRestClientBuilder(
                new ObjectMapper(), Duration.ofSeconds(2), Duration.ofSeconds(250));
    server = MockRestServiceServer.bindTo(builder).build();
    client = new MessageTranslationClient(builder, "server-only-token");
  }

  @Test
  void usesInternalCredentialsStoredTextAndAutomaticDetection() {
    server
        .expect(requestTo("http://ai-service/internal/ai/translate"))
        .andExpect(header("X-Internal-Token", "server-only-token"))
        .andExpect(content().json("{\"text\":\"Hello\",\"targetLanguage\":\"pl-PL\"}"))
        .andRespond(
            withSuccess(
                "{\"translatedText\":\"Cześć\",\"sourceLanguage\":\"auto\",\"targetLanguage\":\"pl-PL\",\"model\":\"translategemma:4b\"}",
                MediaType.APPLICATION_JSON));
    assertThat(client.translate("Hello", "pl-pl").translatedText()).isEqualTo("Cześć");
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "auto", "und", "x-private", "en_US", "not a language"})
  void rejectsInvalidTargetsWithoutCallingAi(String language) {
    assertStatus(() -> client.translate("secret", language), 400);
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(ints = {403, 500, 502, 503, 504})
  void sanitizesBackendErrors(int status) {
    server
        .expect(requestTo("http://ai-service/internal/ai/translate"))
        .andRespond(withStatus(HttpStatus.valueOf(status)).body("private backend content"));
    assertStatus(
        () -> client.translate("Hello", "pl"), status == 504 ? 504 : status == 502 ? 502 : 503);
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"translatedText\":\"\",\"sourceLanguage\":\"auto\",\"targetLanguage\":\"pl\"}",
        "{\"translatedText\":\"Hello\",\"sourceLanguage\":\"auto\",\"targetLanguage\":\"de\"}"
      })
  void rejectsIncompleteOrWrongTargetResponse(String response) {
    server
        .expect(requestTo("http://ai-service/internal/ai/translate"))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    assertStatus(() -> client.translate("Hello", "pl"), 502);
  }

  private void assertStatus(Runnable action, int status) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            error -> {
              assertThat(error.getStatusCode().value()).isEqualTo(status);
              assertThat(error.getReason()).doesNotContain("private backend content", "secret");
            });
  }
}
