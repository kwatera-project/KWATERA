package io.github.kwatera_project.kwatera.billing_service.service;

import io.github.kwatera_project.kwatera.billing_service.model.MediaReading;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingSource;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaReadingService {

  private final MediaReadingRepository mediaReadingRepository;
  private final SettlementService settlementService;

  @Transactional
  public MediaReading createFinalizedMediaReadingCharge(
      UUID settlementId,
      UUID unitId,
      UtilityType utilityType,
      BigDecimal initialReading,
      BigDecimal initialConfidenceScore,
      BigDecimal finalReading,
      BigDecimal finalConfidenceScore,
      BigDecimal unitPrice,
      ReadingSource readingSource,
      ReadingStatus readingStatus) {
    if (finalReading == null) {
      throw new IllegalArgumentException("Final reading is required");
    }

    if (finalReading.compareTo(initialReading) < 0) {
      throw new IllegalArgumentException("Final reading cannot be lower than initial reading");
    }

    BigDecimal consumption = finalReading.subtract(initialReading);
    SettlementItemType itemType = mapUtilityType(utilityType);
    SettlementItem item =
        settlementService.addUtilityCharge(
            settlementId, unitId, itemType, descriptionFor(utilityType), consumption, unitPrice);

    MediaReading reading = new MediaReading();
    reading.setSettlementItemId(item.getId());
    reading.setUtilityType(utilityType);
    reading.setInitialReading(initialReading);
    reading.setInitialConfidenceScore(initialConfidenceScore);
    reading.setFinalReading(finalReading);
    reading.setFinalConfidenceScore(finalConfidenceScore);
    reading.setUnitPrice(unitPrice);
    reading.setReadingSource(readingSource);
    reading.setReadingStatus(readingStatus);

    return mediaReadingRepository.save(reading);
  }

  private SettlementItemType mapUtilityType(UtilityType utilityType) {
    return switch (utilityType) {
      case WATER -> SettlementItemType.WATER;
      case ELECTRICITY -> SettlementItemType.ELECTRICITY;
    };
  }

  private String descriptionFor(UtilityType utilityType) {
    return switch (utilityType) {
      case WATER -> "Water usage";
      case ELECTRICITY -> "Electricity usage";
    };
  }
}
