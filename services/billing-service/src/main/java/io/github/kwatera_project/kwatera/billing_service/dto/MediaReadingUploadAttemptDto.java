package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.MediaReadingUploadAttempt;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record MediaReadingUploadAttemptDto(
    UUID id,
    UUID mediaReadingId,
    String imageBase64,
    String ocrValue,
    BigDecimal confidenceScore,
    ReadingStatus status,
    Instant attemptedAt,
    ReadingType readingType) {
  public static MediaReadingUploadAttemptDto from(MediaReadingUploadAttempt attempt) {
    return new MediaReadingUploadAttemptDto(
        attempt.getId(),
        attempt.getMediaReadingId(),
        attempt.getMeterImage() != null
            ? Base64.getEncoder().encodeToString(attempt.getMeterImage())
            : null,
        attempt.getOcrValue(),
        attempt.getConfidenceScore(),
        attempt.getStatus(),
        attempt.getAttemptedAt(),
        attempt.getReadingType());
  }
}
