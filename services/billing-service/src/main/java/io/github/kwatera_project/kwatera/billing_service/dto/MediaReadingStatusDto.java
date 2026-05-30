package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.MediaReading;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingSource;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import java.math.BigDecimal;
import java.util.UUID;

public record MediaReadingStatusDto(
    UUID id,
    UUID settlementId,
    UtilityType utilityType,
    BigDecimal initialReading,
    BigDecimal finalReading,
    BigDecimal initialConfidenceScore,
    BigDecimal finalConfidenceScore,
    ReadingStatus initialReadingStatus,
    ReadingStatus finalReadingStatus,
    ReadingSource initialReadingSource,
    ReadingSource finalReadingSource) {

  public static MediaReadingStatusDto from(MediaReading reading) {
    return new MediaReadingStatusDto(
        reading.getId(),
        reading.getSettlementId(),
        reading.getUtilityType(),
        reading.getInitialReading(),
        reading.getFinalReading(),
        reading.getInitialConfidenceScore(),
        reading.getFinalConfidenceScore(),
        reading.getInitialReadingStatus(),
        reading.getFinalReadingStatus(),
        reading.getInitialReadingSource(),
        reading.getFinalReadingSource());
  }
}
