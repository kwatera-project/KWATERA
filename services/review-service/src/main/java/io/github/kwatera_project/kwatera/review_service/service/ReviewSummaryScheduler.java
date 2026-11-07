package io.github.kwatera_project.kwatera.review_service.service;

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

@Component
public class ReviewSummaryScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReviewSummaryScheduler.class);

  private final ReviewRepository reviewRepository;
  private Map<UUID, List<ReviewSummaryInput>> previousInputs;

  public ReviewSummaryScheduler(ReviewRepository reviewRepository) {
    this.reviewRepository = reviewRepository;
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

      if (!before.equals(after)) {
        changedUnits++;
        log.info(
            "Review summary inputs changed for unit {}: {} comments before, {} now",
            unitId,
            before.size(),
            after.size());
      }
    }

    previousInputs = currentInputs;
    log.info("Review summary scan finished: {} changed units", changedUnits);
  }
}
