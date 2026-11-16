package io.github.kwatera_project.kwatera.review_service.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class ReviewTranslationClientTest {

  @Mock private DeepLClient deepLClient;
  @Mock private TextResult textResult;

  @Test
  void detectsSourceAndTranslatesWithTheOfficialDeepLClient() throws Exception {
    when(deepLClient.translateText("Great room.", null, "PL")).thenReturn(textResult);
    when(textResult.getText()).thenReturn("Świetny pokój.");
    when(textResult.getDetectedSourceLanguage()).thenReturn("EN");
    var client = clientWithMockDeepL("test-key");

    var result = client.translate("Great room.", "pl");

    assertEquals("Świetny pokój.", result.translatedText());
    assertEquals("EN", result.sourceLanguage());
    assertFalse(result.alreadyInTargetLanguage());
    verify(deepLClient).translateText("Great room.", null, "PL");
  }

  @Test
  void returnsOriginalWhenDetectedSourceMatchesTarget() throws Exception {
    when(deepLClient.translateText("Cześć, pokój!", null, "PL")).thenReturn(textResult);
    when(textResult.getText()).thenReturn("Cześć, pokój!");
    when(textResult.getDetectedSourceLanguage()).thenReturn("PL");
    var client = clientWithMockDeepL("test-key");

    var result = client.translate("Cześć, pokój!", "pl");

    assertEquals("Cześć, pokój!", result.translatedText());
    assertEquals("PL", result.sourceLanguage());
    assertTrue(result.alreadyInTargetLanguage());
  }

  @Test
  void mapsEnglishUiLanguageToRegionalDeepLLanguage() throws Exception {
    when(deepLClient.translateText("Dobry pobyt.", null, "en-US")).thenReturn(textResult);
    when(textResult.getText()).thenReturn("A good stay.");
    when(textResult.getDetectedSourceLanguage()).thenReturn("PL");

    var result = clientWithMockDeepL("test-key").translate("Dobry pobyt.", "en");

    assertEquals("A good stay.", result.translatedText());
    assertFalse(result.alreadyInTargetLanguage());
    verify(deepLClient).translateText("Dobry pobyt.", null, "en-US");
  }

  @Test
  void recognizesSameLanguageEvenWhenDetectedLanguageHasRegionalVariant() throws Exception {
    when(deepLClient.translateText("Hello.", null, "en-US")).thenReturn(textResult);
    when(textResult.getText()).thenReturn("Hello.");
    when(textResult.getDetectedSourceLanguage()).thenReturn("EN-GB");

    var result = clientWithMockDeepL("test-key").translate("Hello.", "en");

    assertEquals("Hello.", result.translatedText());
    assertEquals("EN-GB", result.sourceLanguage());
    assertTrue(result.alreadyInTargetLanguage());
  }

  @Test
  void usesDeepLTranslationWhenDetectedLanguageIsMissing() throws Exception {
    when(deepLClient.translateText("A nice room.", null, "PL")).thenReturn(textResult);
    when(textResult.getText()).thenReturn("Miły pokój.");
    when(textResult.getDetectedSourceLanguage()).thenReturn(null);

    var result = clientWithMockDeepL("test-key").translate("A nice room.", "pl");

    assertEquals("Miły pokój.", result.translatedText());
    assertNull(result.sourceLanguage());
    assertFalse(result.alreadyInTargetLanguage());
  }

  @Test
  void rejectsNullDeepLResponse() throws Exception {
    when(deepLClient.translateText("Hello.", null, "PL")).thenReturn(null);

    assertThrows(
        RestClientException.class, () -> clientWithMockDeepL("test-key").translate("Hello.", "pl"));
  }

  @Test
  void rejectsBlankDeepLTranslation() throws Exception {
    when(deepLClient.translateText("Hello.", null, "PL")).thenReturn(textResult);
    when(textResult.getText()).thenReturn("  ");

    assertThrows(
        RestClientException.class, () -> clientWithMockDeepL("test-key").translate("Hello.", "pl"));
  }

  @Test
  void wrapsDeepLFailures() throws Exception {
    when(deepLClient.translateText("Hello.", null, "PL"))
        .thenThrow(new DeepLException("DeepL unavailable"));

    assertThrows(
        RestClientException.class, () -> clientWithMockDeepL("test-key").translate("Hello.", "pl"));
  }

  @Test
  void restoresInterruptFlagWhenDeepLCallIsInterrupted() throws Exception {
    when(deepLClient.translateText("Hello.", null, "PL"))
        .thenThrow(new InterruptedException("interrupted"));

    try {
      assertThrows(
          RestClientException.class,
          () -> clientWithMockDeepL("test-key").translate("Hello.", "pl"));
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void lazilyCreatesAndReusesDeepLClient() {
    var client = new ReviewTranslationClient("test-key", Duration.ofSeconds(15));

    var firstClient = ReflectionTestUtils.<DeepLClient>invokeMethod(client, "getDeepLClient");
    var secondClient = ReflectionTestUtils.<DeepLClient>invokeMethod(client, "getDeepLClient");

    assertNotNull(firstClient);
    assertSame(firstClient, secondClient);
  }

  @Test
  void rejectsMissingApiKeyBeforeCallingDeepL() {
    var client = new ReviewTranslationClient("", Duration.ofSeconds(15));

    assertThrows(RestClientException.class, () -> client.translate("Great room.", "pl"));
  }

  private ReviewTranslationClient clientWithMockDeepL(String apiKey) {
    var client = new ReviewTranslationClient(apiKey, Duration.ofSeconds(15));
    ReflectionTestUtils.setField(client, "deepLClient", deepLClient);
    return client;
  }
}
