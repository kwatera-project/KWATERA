package io.github.kwatera_project.kwatera.review_service.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

@Component
public final class ReviewTranslationClient {

    private static final String TRANSLATION_URL =
            "http://ai-service/internal/ai/translate";

    private final RestOperations restOperations;
    private final String internalToken;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "RestOperations is a shared Spring-managed dependency")
    public ReviewTranslationClient(
            @Qualifier("reviewAiRestTemplate") RestOperations restOperations,
            @Value("${kwatera.ai.internal-token:}") String internalToken) {
        this.restOperations = restOperations;
        this.internalToken = internalToken;
    }

    public String translate(String text, String targetLanguage) {
        if (internalToken.isBlank()) {
            throw new RestClientException("Review AI internal token is missing");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Token", internalToken);

        var body = new AiTranslationRequest(text, "auto", targetLanguage);
        var request = new HttpEntity<>(body, headers);

        AiTranslationResponse response =
                restOperations.postForObject(
                        TRANSLATION_URL, request, AiTranslationResponse.class);

        if (response == null
                || response.translatedText() == null
                || response.translatedText().isBlank()) {
            throw new RestClientException("AI service returned an empty translation");
        }

        return response.translatedText();
    }

    private record AiTranslationRequest(
            String text,
            String sourceLanguage,
            String targetLanguage) {}

  public record AiTranslationResponse(
            String translatedText,
            String sourceLanguage,
            String targetLanguage,
            String model) {}
}
