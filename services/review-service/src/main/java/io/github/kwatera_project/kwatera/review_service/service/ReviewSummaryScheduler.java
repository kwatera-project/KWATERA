package io.github.kwatera_project.kwatera.review_service.service;

import io.github.kwatera_project.kwatera.review_service.client.ReviewAiClient;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewSummaryInput;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class ReviewSummaryScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReviewSummaryScheduler.class);

  private final ReviewRepository reviewRepository;
  private final ReviewAiClient reviewAiClient;
  private Map<UUID, List<ReviewSummaryInput>> previousInputs;

  public ReviewSummaryScheduler(ReviewRepository reviewRepository, ReviewAiClient reviewAiClient) {
    this.reviewRepository = reviewRepository;
    this.reviewAiClient = reviewAiClient;
  }

  @Scheduled(cron = "${kwatera.review-summary.cron:-}", zone = "Europe/Warsaw")
  public void checkForChanges() {
    Map<UUID, List<ReviewSummaryInput>> currentInputs = new HashMap<>();

    for (UUID unitId : reviewRepository.findUnitIdsWithReviews()) {
      List<ReviewSummaryInput> inputs =
          reviewRepository.findSummaryInputsByUnitIds(List.of(unitId));

      if (!inputs.isEmpty()) {
        currentInputs.put(unitId, inputs);
      }
    }

    if (previousInputs == null) {
      previousInputs = currentInputs;
      log.info("Review summary initial scan: {} units with comments", currentInputs.size());
      return;
    }

    Set<UUID> unitIds = new HashSet<>(previousInputs.keySet());
    unitIds.addAll(currentInputs.keySet());

    int changedUnits = 0;

    for (UUID unitId : unitIds) {
      List<ReviewSummaryInput> before = previousInputs.getOrDefault(unitId, List.of());
      List<ReviewSummaryInput> after = currentInputs.getOrDefault(unitId, List.of());

      if (before.equals(after)) {
        continue;
      }

      changedUnits++;

      if (after.isEmpty()) {
        previousInputs.remove(unitId);
        log.info("No review comments left for unit {}", unitId);
        continue;
      }

      List<String> comments = after.stream().map(ReviewSummaryInput::comment).toList();

      try {
        String summary = reviewAiClient.summarize(comments, "pl");
        previousInputs.put(unitId, after);
        log.info(
            "AI summary generated for unit {} from {} comments ({} characters)",
            unitId,
            comments.size(),
            summary.length());
      } catch (RestClientException | IllegalStateException exception) {
        log.warn("Could not generate AI summary for unit {}: {}", unitId, exception.getMessage());
      }
    }

    log.info("Review summary scan finished: {} changed units", changedUnits);
  }
}
