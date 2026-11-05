package io.github.kwatera_project.kwatera.reservation_service.conversation.client;

import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.MessageTranslationResponse;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MessageTranslationClient {
  private final RestClient client;
  private final String internalToken;

  public MessageTranslationClient(
      @Qualifier("translationRestClientBuilder") RestClient.Builder builder,
      @Value("${kwatera.ai.internal-token:}") String internalToken) {
    this.client = builder.build();
    this.internalToken = internalToken;
  }

  public MessageTranslationResponse translate(String content, String targetLanguage) {
    String target = canonicalLanguage(targetLanguage);
    if (internalToken.isBlank()) throw unavailable();
    try {
      var result =
          client
              .post()
              .uri("http://ai-service/internal/ai/translate")
              .header("X-Internal-Token", internalToken)
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("text", content, "targetLanguage", target))
              .retrieve()
              .body(MessageTranslationResponse.class);
      if (result == null
          || result.translatedText() == null
          || result.translatedText().isBlank()
          || result.translatedText().length() > 32000
          || result.sourceLanguage() == null
          || !target.equalsIgnoreCase(result.targetLanguage())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "Invalid AI translation response");
      }
      return result;
    } catch (RestClientResponseException exception) {
      // Never relay backend bodies, credentials or message contents to the caller.
      if (exception.getStatusCode().value() == 504) {
        throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Translation timed out");
      }
      if (exception.getStatusCode().value() == 502) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "Invalid AI translation response");
      }
      throw unavailable();
    } catch (RestClientException | IllegalStateException exception) {
      throw unavailable();
    }
  }

  private String canonicalLanguage(String language) {
    try {
      if (language == null || language.length() > 64 || language.equalsIgnoreCase("auto")) {
        throw new IllformedLocaleException();
      }
      Locale locale = new Locale.Builder().setLanguageTag(language).build();
      if (locale.getLanguage().isEmpty() || locale.getLanguage().equals("und")) {
        throw new IllformedLocaleException();
      }
      return locale.toLanguageTag();
    } catch (IllformedLocaleException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target language");
    }
  }

  private ResponseStatusException unavailable() {
    return new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "Translation is temporarily unavailable");
  }
}
