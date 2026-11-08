package io.github.kwatera_project.kwatera.review_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.kwatera_project.kwatera.review_service.dto.ReviewSummaryInput;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewSummaryFingerprintTest {

  private static final Instant ORIGINAL_TIME = Instant.parse("2026-11-07T12:00:00Z");

  private final ReviewSummaryInput first =
      new ReviewSummaryInput(UUID.randomUUID(), "Czysty pokój.", ORIGINAL_TIME);
  private final ReviewSummaryInput second =
      new ReviewSummaryInput(UUID.randomUUID(), "Wygodne łóżko.", ORIGINAL_TIME);

  @Test
  void shouldReturnSameFingerprintRegardlessOfReviewOrder() {
    assertEquals(
        ReviewSummaryFingerprint.calculate(List.of(first, second)),
        ReviewSummaryFingerprint.calculate(List.of(second, first)));
  }

  @Test
  void shouldChangeFingerprintWhenSameReviewGetsNewTimestamp() {
    ReviewSummaryInput updated =
        new ReviewSummaryInput(first.id(), first.comment(), ORIGINAL_TIME.plusSeconds(60));

    assertNotEquals(
        ReviewSummaryFingerprint.calculate(List.of(first, second)),
        ReviewSummaryFingerprint.calculate(List.of(updated, second)));
  }

  @Test
  void shouldChangeFingerprintWhenAReviewIsAddedOrRemoved() {
    assertNotEquals(
        ReviewSummaryFingerprint.calculate(List.of(first)),
        ReviewSummaryFingerprint.calculate(List.of(first, second)));
  }
}
