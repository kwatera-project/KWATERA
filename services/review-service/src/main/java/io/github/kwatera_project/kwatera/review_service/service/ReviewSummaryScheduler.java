package io.github.kwatera_project.kwatera.review_service.service;

import io.github.kwatera_project.kwatera.review_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.review_service.client.ReviewAiClient;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewSummaryInput;
import io.github.kwatera_project.kwatera.review_service.model.ReviewSummary;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewSummaryRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

@Component
public class ReviewSummaryScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReviewSummaryScheduler.class);

  private static final String SUMMARY_LANGUAGE = "en";

  private final ReviewRepository reviewRepository;
  private final ReviewAiClient reviewAiClient;
  private final PropertyClient propertyClient;
  private final ReviewSummaryRepository summaries;
  private final ObjectWriter summaryWriter;

  public ReviewSummaryScheduler(
      ReviewRepository reviewRepository,
      ReviewAiClient reviewAiClient,
      PropertyClient propertyClient,
      ReviewSummaryRepository summaries,
      ObjectMapper mapper) {
    this.reviewRepository = reviewRepository;
    this.reviewAiClient = reviewAiClient;
    this.propertyClient = propertyClient;
    this.summaries = summaries;
    this.summaryWriter = mapper.writer();
  }

  @Scheduled(cron = "${kwatera.review-summary.cron:-}", zone = "Europe/Warsaw")
  public void checkForChanges() {
    Set<UUID> unitIds = new HashSet<>(reviewRepository.findUnitIdsWithReviews());
    unitIds.addAll(summaries.findCachedUnitIds());
    Set<UUID> propertyIds = new HashSet<>(summaries.findCachedPropertyIds());

    for (UUID unitId : unitIds) {
      refreshUnit(unitId);

      try {
        propertyIds.add(propertyClient.getPropertyIdForUnit(unitId));
      } catch (ResponseStatusException | IllegalStateException exception) {
        log.warn("Could not find property for unit {}", unitId, exception);
      }
    }

    for (UUID propertyId : propertyIds) {
      refreshProperty(propertyId);
    }
  }

  private void refreshUnit(UUID unitId) {
    try {
      var inputs = reviewRepository.findSummaryInputsByUnitIds(List.of(unitId));
      var summary = summaries.findByUnitId(unitId).orElseGet(ReviewSummary::new);
      summary.setUnitId(unitId);
      refresh(summary, inputs);
    } catch (RestClientException | IllegalStateException | DataAccessException exception) {
      log.warn("Could not refresh AI summary for unit {}", unitId, exception);
    }
  }

  private void refreshProperty(UUID propertyId) {
    try {
      var unitIdsForProperty = propertyClient.getUnitIdsForProperty(propertyId);
      List<ReviewSummaryInput> inputs =
          unitIdsForProperty.isEmpty()
              ? List.of()
              : reviewRepository.findSummaryInputsByUnitIds(unitIdsForProperty);
      var summary = summaries.findByPropertyId(propertyId).orElseGet(ReviewSummary::new);
      summary.setPropertyId(propertyId);
      refresh(summary, inputs);
    } catch (ResponseStatusException
        | RestClientException
        | IllegalStateException
        | DataAccessException exception) {
      log.warn("Could not refresh AI summary for property {}", propertyId, exception);
    }
  }

  private void refresh(ReviewSummary summary, List<ReviewSummaryInput> inputs) {
    if (inputs.isEmpty()) {
      if (summary.getId() != null) {
        summaries.delete(summary);
      }
      return;
    }

    String fingerprint = SUMMARY_LANGUAGE + ":v5:" + ReviewSummaryFingerprint.calculate(inputs);
    if (fingerprint.equals(summary.getInputFingerprint())) {
      return;
    }

    var comments = inputs.stream().map(ReviewSummaryInput::comment).toList();
    var result = reviewAiClient.summarize(comments, SUMMARY_LANGUAGE);
    summary.setAiSummary(summaryWriter.writeValueAsString(result));
    summary.setReviewIds(inputs.stream().map(ReviewSummaryInput::id).toList());
    summary.setInputFingerprint(fingerprint);
    summary.setGenerationTimestamp(Instant.now());
    summaries.save(summary);
    log.info(
        "AI summary saved for {} {} from {} comments",
        summary.getUnitId() == null ? "property" : "unit",
        summary.getUnitId() == null ? summary.getPropertyId() : summary.getUnitId(),
        inputs.size());
  }
}
