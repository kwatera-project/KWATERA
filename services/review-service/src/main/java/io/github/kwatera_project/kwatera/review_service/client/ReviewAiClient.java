package io.github.kwatera_project.kwatera.review_service.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
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
public final class ReviewAiClient {

  private static final String SUMMARY_URL = "http://ai-service/internal/ai/summarize";

  private final RestOperations restOperations;
  private final String internalToken;

  private final int maxReviews;
  private final int maxCharacters;
  private final int maxTotal;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "RestOperations is a shared Spring-managed dependency")
  public ReviewAiClient(
      @Qualifier("reviewAiRestTemplate") RestOperations restOperations,
      @Value("${kwatera.ai.internal-token:}") String internalToken,
      @Value("${AI_MAX_REVIEWS:20}") int maxReviews,
      @Value("${AI_MAX_REVIEW_CHARACTERS:2000}") int maxCharacters,
      @Value("${AI_MAX_TOTAL_REVIEW_CHARACTERS:6000}") int maxTotal) {
    this.restOperations = restOperations;
    this.internalToken = internalToken;
    if (maxReviews < 1 || maxCharacters < 1 || maxTotal < 1) {
      throw new IllegalArgumentException("AI input limits must be positive");
    }
    this.maxReviews = maxReviews;
    this.maxCharacters = Math.min(maxCharacters, maxTotal);
    this.maxTotal = maxTotal;
  }

  public SummaryResponse summarize(List<String> comments, String language) {
    if (comments.isEmpty()) {
      throw new IllegalArgumentException("At least one review comment is required");
    }

    if (internalToken.isBlank()) {
      throw new RestClientException("Review AI internal token is missing");
    }

    var batches = partition(comments);
    while (true) {
      List<SummaryResponse> batchSummaries = new ArrayList<>();
      for (var batch : batches) {
        batchSummaries.add(requestSummary(batch, language));
      }
      if (batchSummaries.size() == 1) {
        return batchSummaries.getFirst();
      }

      var notes = batchSummaries.stream().map(SummaryResponse::asNotes).toList();
      var nextBatches = partition(notes);
      if (nextBatches.size() >= batches.size()) {
        return mergeInPairs(batchSummaries, language);
      }
      batches = nextBatches;
    }
  }

  private SummaryResponse mergeInPairs(List<SummaryResponse> summaries, String language) {
    while (summaries.size() > 1) {
      List<SummaryResponse> merged = new ArrayList<>();
      for (int i = 0; i < summaries.size(); i += 2) {
        var left = summaries.get(i);
        if (i + 1 == summaries.size()) {
          merged.add(left);
          continue;
        }

        var right = summaries.get(i + 1);
        int budget =
            maxReviews == 1 ? (maxCharacters - 1) / 2 : Math.min(maxCharacters, maxTotal / 2);
        String first = left.mergeNote(budget);
        String second = right.mergeNote(budget);
        List<String> pair =
            maxReviews == 1 ? List.of(first + "\n" + second) : List.of(first, second);
        merged.add(requestSummary(pair, language));
      }
      summaries = merged;
    }
    return summaries.getFirst();
  }

  private List<List<String>> partition(List<String> texts) {
    List<List<String>> batches = new ArrayList<>();
    List<String> batch = new ArrayList<>();
    int characters = 0;
    for (String text : texts) {
      int start = 0;
      while (start < text.length()) {
        int end = fragmentEnd(text, start);
        String part = text.substring(start, end);
        start = end;
        if (part.isBlank()) {
          continue;
        }
        if (batch.size() == maxReviews || part.length() > maxTotal - characters) {
          batches.add(batch);
          batch = new ArrayList<>();
          characters = 0;
        }
        batch.add(part);
        characters += part.length();
      }
    }
    if (batch.isEmpty()) {
      throw new IllegalArgumentException("At least one nonblank review comment is required");
    }
    batches.add(batch);
    return batches;
  }

  private int fragmentEnd(String text, int start) {
    int end = start + Math.min(maxCharacters, text.length() - start);
    if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) {
      end--;
    }
    if (end == start) {
      throw new IllegalArgumentException("AI character limit cannot fit a Unicode character");
    }
    return end;
  }

  private SummaryResponse requestSummary(List<String> comments, String language) {
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

    return response;
  }

  private record SummaryRequest(List<String> reviews, String language) {
    private SummaryRequest {
      reviews = List.copyOf(reviews);
    }
  }

  public record SummaryResponse(
      String summary, List<String> pros, List<String> cons, String model) {
    private static final int MAX_SUMMARY_WORDS = 45;

    public SummaryResponse {
      summary = limitSummaryWords(summary);
      validatePoints(pros);
      pros = List.copyOf(pros);
      validatePoints(cons);
      cons = List.copyOf(cons);
    }

    private static String limitSummaryWords(String summary) {
      if (summary == null) {
        return null;
      }
      String[] words = summary.strip().split("\\s+", MAX_SUMMARY_WORDS + 1);
      if (words.length <= MAX_SUMMARY_WORDS) {
        return summary;
      }
      for (int end = MAX_SUMMARY_WORDS; end > 0; end--) {
        if (words[end - 1].matches(".*[.!?]$")) {
          return String.join(" ", Arrays.copyOf(words, end));
        }
      }
      return String.join(" ", Arrays.copyOf(words, MAX_SUMMARY_WORDS)) + "…";
    }

    private String mergeNote(int budget) {
      String notes = asNotes();
      if (notes.length() <= budget) {
        return notes;
      }
      if (summary.length() <= budget) {
        var note = new StringBuilder(summary);
        for (int i = 0; i < Math.max(pros.size(), cons.size()); i++) {
          appendPointIfItFits(note, pros, i, "\n+ ", budget);
          appendPointIfItFits(note, cons, i, "\n- ", budget);
        }
        return note.toString();
      }
      int end = Math.max(0, budget);
      if (end > 0 && Character.isHighSurrogate(summary.charAt(end - 1))) {
        end--;
      }
      String excerpt = summary.substring(0, end).strip();
      if (excerpt.isEmpty()) {
        throw new RestClientException("AI input limit is too small to merge two summaries");
      }
      int space = excerpt.lastIndexOf(' ');
      return space > 0 ? excerpt.substring(0, space) : excerpt;
    }

    private static void appendPointIfItFits(
        StringBuilder note, List<String> points, int index, String prefix, int budget) {
      if (index < points.size()
          && points.get(index).length() <= budget - note.length() - prefix.length()) {
        note.append(prefix).append(points.get(index));
      }
    }

    private String asNotes() {
      return String.join(
          "\n", summary, "Pros: " + String.join("; ", pros), "Cons: " + String.join("; ", cons));
    }

    private static void validatePoints(List<String> points) {
      if (points == null || points.stream().anyMatch(point -> point == null || point.isBlank())) {
        throw new IllegalArgumentException("Expected a list of nonblank review points");
      }
    }
  }
}
