package io.github.kwatera_project.kwatera.ai_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.ai_service.error.AiException;
import io.github.kwatera_project.kwatera.ai_service.service.AiInference;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.json.JsonMapper;

class AiInferenceTest {
  private final ChatModel model = mock(ChatModel.class);
  private final AiInference inference =
      new AiInference(model, JsonMapper.builder().build(), OllamaConfigurationTest.properties());

  private void rejects(String code) {
    assertThatThrownBy(
            () -> inference.generate("Summarize", Map.of("reviews", List.of("private")), "summary"))
        .isInstanceOfSatisfying(
            AiException.class, exception -> assertThat(exception.code()).isEqualTo(code));
  }

  @Test
  void rejectsEmptyGenerationOversizedOutputAndTruncatedButValidJson() {
    when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of()));
    rejects("AI_INVALID_RESPONSE");
    when(model.call(any(Prompt.class)))
        .thenReturn(
            new ChatResponse(
                List.of(
                    new Generation(
                        new AssistantMessage("{\"summary\":\"" + "x".repeat(16001) + "\"}")))));
    rejects("AI_INVALID_RESPONSE");
    when(model.call(any(Prompt.class)))
        .thenReturn(
            new ChatResponse(
                List.of(
                    new Generation(
                        new AssistantMessage("{\"summary\":\"unfinished\"}"),
                        ChatGenerationMetadata.builder().finishReason("length").build()))));
    rejects("AI_INVALID_RESPONSE");
  }

  @Test
  void rejectsTruncatedAndOversizedPlainTranslationAndPreservesValidMultilineText() {
    when(model.call(any(Prompt.class)))
        .thenReturn(
            new ChatResponse(
                List.of(
                    new Generation(
                        new AssistantMessage("unfinished"),
                        ChatGenerationMetadata.builder().finishReason("length").build()))));
    assertThatThrownBy(() -> inference.translateText("translate"))
        .isInstanceOfSatisfying(
            AiException.class,
            exception -> assertThat(exception.code()).isEqualTo("AI_INVALID_RESPONSE"));
    when(model.call(any(Prompt.class)))
        .thenReturn(
            new ChatResponse(List.of(new Generation(new AssistantMessage("x".repeat(16001))))));
    assertThatThrownBy(() -> inference.translateText("translate"))
        .isInstanceOfSatisfying(
            AiException.class,
            exception -> assertThat(exception.code()).isEqualTo("AI_INVALID_RESPONSE"));
    when(model.call(any(Prompt.class)))
        .thenReturn(
            new ChatResponse(List.of(new Generation(new AssistantMessage("Cześć\n\nŁ-0042")))));
    assertThat(inference.translateText("translate")).isEqualTo("Cześć\n\nŁ-0042");
  }

  @Test
  void classifiesBackendStatusAndTransportErrorsWithoutRetrying() {
    when(model.call(any(Prompt.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));
    rejects("AI_UNAVAILABLE");
    when(model.call(any(Prompt.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT));
    rejects("AI_TIMEOUT");
    when(model.call(any(Prompt.class)))
        .thenThrow(new ResourceAccessException("private", new HttpTimeoutException("private")));
    rejects("AI_TIMEOUT");
    when(model.call(any(Prompt.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
    rejects("AI_INVALID_RESPONSE");
    when(model.call(any(Prompt.class))).thenThrow(new IllegalStateException("private"));
    rejects("AI_INVALID_RESPONSE");
    verify(model, times(5)).call(any(Prompt.class));
  }
}
