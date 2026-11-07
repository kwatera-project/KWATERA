package io.github.kwatera_project.kwatera.review_service.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

@Component
public class ReviewAiClient {

  private static final String SUMMARY_URL = "http://ai-service/internal/ai/summarize";

  private final RestOperations restOperations;
  private final String internalToken;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "RestOperations is a shared Spring-managed dependency")
  public ReviewAiClient(
      @Qualifier("reviewAiRestTemplate") RestOperations restOperations,
      @Value("${kwatera.ai.internal-token:}") String internalToken) {
    this.restOperations = restOperations;
    this.internalToken = internalToken;
  }

  public String summarize(List<String> comments, String language) {
    if (comments.isEmpty()) {
      throw new IllegalArgumentException("At least one review comment is required");
    }

    if (internalToken.isBlank()) {
      throw new RestClientException("Review AI internal token is missing");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Token", internalToken);

    SummaryRequest body = new SummaryRequest(comments, language);
    HttpEntity<SummaryRequest> request = new HttpEntity<>(body, headers);

    SummaryResponse response =
        restOperations.postForObject(SUMMARY_URL, request, SummaryResponse.class);

    if (response == null
        || response.summary() == null
        || response.summary().isBlank()
        || response.model() == null
        || response.model().isBlank()) {
      throw new RestClientException("AI service returned an invalid review summary");
    }

    return response.summary();
  }

  private record SummaryRequest(List<String> reviews, String language) {
    private SummaryRequest {
      reviews = List.copyOf(reviews);
    }
  }

  private record SummaryResponse(String summary, String model) {}
}
