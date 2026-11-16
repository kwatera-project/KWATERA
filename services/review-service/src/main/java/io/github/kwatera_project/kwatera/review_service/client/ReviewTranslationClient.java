package io.github.kwatera_project.kwatera.review_service.client;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLClientOptions;
import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public final class ReviewTranslationClient {

  private final String apiKey;
  private final Duration timeout;
  private DeepLClient deepLClient;

  @Autowired
  public ReviewTranslationClient(
      @Value("${kwatera.deepl.api-key:}") String apiKey,
      @Value("${kwatera.deepl.timeout:15s}") Duration timeout) {
    this.apiKey = apiKey;
    this.timeout = timeout;
  }

  public TranslationResult translate(String text, String targetLanguage) {
    if (apiKey.isBlank()) {
      throw new RestClientException("DeepL API key is missing");
    }

    try {
      String deepLTargetLanguage = toDeepLTargetLanguage(targetLanguage);
      TextResult result = getDeepLClient().translateText(text, null, deepLTargetLanguage);
      if (result == null || result.getText() == null || result.getText().isBlank()) {
        throw new RestClientException("DeepL returned an empty translation");
      }

      String detectedLanguage = result.getDetectedSourceLanguage();
      boolean alreadyInTargetLanguage =
          detectedLanguage != null
              && baseLanguage(detectedLanguage).equals(baseLanguage(targetLanguage));
      return new TranslationResult(
          alreadyInTargetLanguage ? text : result.getText(),
          detectedLanguage,
          alreadyInTargetLanguage);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RestClientException("DeepL translation request failed", exception);
    } catch (DeepLException exception) {
      throw new RestClientException("DeepL translation request failed", exception);
    }
  }

  private String toDeepLTargetLanguage(String targetLanguage) {
    if ("en".equalsIgnoreCase(targetLanguage)) {
      return "en-US";
    }
    return targetLanguage.toUpperCase(Locale.ROOT);
  }

  private String baseLanguage(String language) {
    int separator = language.indexOf('-');
    String base = separator >= 0 ? language.substring(0, separator) : language;
    return base.toLowerCase(Locale.ROOT);
  }

  private synchronized DeepLClient getDeepLClient() {
    if (deepLClient == null) {
      var options = new DeepLClientOptions().setMaxRetries(0).setTimeout(timeout);
      deepLClient = new DeepLClient(apiKey, options);
    }
    return deepLClient;
  }

  public record TranslationResult(
      String translatedText, String sourceLanguage, boolean alreadyInTargetLanguage) {}
}
