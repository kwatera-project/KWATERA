package io.github.kwatera_project.kwatera.review_service.service;

import io.github.kwatera_project.kwatera.review_service.client.PropertyClient;
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
import org.springframework.web.server.ResponseStatusException;

@Component
public class ReviewSummaryScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReviewSummaryScheduler.class);

  private final ReviewRepository reviewRepository;
  private final ReviewAiClient reviewAiClient;
  private final PropertyClient propertyClient;
  private final Set<UUID> pendingUnits = new HashSet<>();
  private final Set<UUID> pendingPropertyLookups = new HashSet<>();
  private final Set<UUID> pendingProperties = new HashSet<>();
  private Map<UUID, List<ReviewSummaryInput>> previousInputs;

  public ReviewSummaryScheduler(
      ReviewRepository reviewRepository, ReviewAiClient reviewAiClient, PropertyClient propertyClient) {
    this.reviewRepository = reviewRepository;
    this.reviewAiClient = reviewAiClient;
    this.propertyClient = propertyClient;
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

      pendingUnits.add(unitId);
      pendingPropertyLookups.add(unitId);
    }
    previousInputs = currentInputs;

    pendingUnits.removeIf(id -> generate("unit", id, currentInputs.getOrDefault(id, List.of())));
    pendingPropertyLookups.removeIf(this::queueProperty);
    pendingProperties.removeIf(this::refreshProperty);
    log.info("Review summary scan finished: {} changed units", changedUnits);
  }

  private boolean queueProperty(UUID unitId) {
    try {
      pendingProperties.add(propertyClient.getPropertyIdForUnit(unitId));
      return true;
    } catch (ResponseStatusException | IllegalStateException exception) {
      log.warn("Could not find property for unit {}: {}", unitId, exception.getMessage());
      return false;
    }
  }

  private boolean refreshProperty(UUID propertyId) {
    try {
      List<UUID> unitIds = propertyClient.getUnitIdsForProperty(propertyId);
      List<ReviewSummaryInput> inputs =
          unitIds.isEmpty() ? List.of() : reviewRepository.findSummaryInputsByUnitIds(unitIds);
      return generate("property", propertyId, inputs);
    } catch (ResponseStatusException | IllegalStateException exception) {
      log.warn("Could not load units for property {}: {}", propertyId, exception.getMessage());
      return false;
    }
  }

  private boolean generate(String scope, UUID id, List<ReviewSummaryInput> inputs) {
    if (inputs.isEmpty()) {
      log.info("No review comments left for {} {}", scope, id);
      return true;
    }

    List<String> comments = inputs.stream().map(ReviewSummaryInput::comment).toList();
    try {
      var result = reviewAiClient.summarize(comments, "pl");
      log.info(
          "AI summary generated for {} {} from {} comments ({} characters)",
          scope, id, comments.size(), result.summary().length());
      return true;
    } catch (RestClientException | IllegalStateException exception) {
      log.warn("Could not generate AI summary for {} {}: {}", scope, id, exception.getMessage());
      return false;
    }
  }
}
