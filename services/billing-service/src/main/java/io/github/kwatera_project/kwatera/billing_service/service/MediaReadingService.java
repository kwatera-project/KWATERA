package io.github.kwatera_project.kwatera.billing_service.service;

import static io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus.PENDING;

import io.github.kwatera_project.kwatera.billing_service.model.MediaReading;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingSource;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingRepository;
import jakarta.persistence.EntityNotFoundException;
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
  public void createMediaReadingWithInitialReading(
      UUID settlementId,
      UtilityType utilityType,
      BigDecimal initialReading,
      BigDecimal initialConfidenceScore,
      BigDecimal unitPrice,
      ReadingSource readingSource) {

    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(utilityType);
    reading.setInitialReading(initialReading);
    reading.setInitialConfidenceScore(initialConfidenceScore);
    reading.setUnitPrice(unitPrice);
    reading.setReadingSource(readingSource);

    // Tu warto dodać sprawdzenie initialConfidenceScore i w zależności od tego ustawić
    // setReadingStatus()

    reading.setReadingStatus(PENDING);

    mediaReadingRepository.save(reading);
  }

  @Transactional
  public void addFinalMediaReading(
      UUID settlementId, UUID unitId, BigDecimal finalReading, BigDecimal finalConfidenceScore) {

    MediaReading reading = mediaReadingRepository.findBySettlementId(settlementId).orElse(null);

    if (reading == null) {
      throw new EntityNotFoundException(
          "Media Reading not found for settlementId: " + settlementId);
    }

    if (finalReading == null) {
      throw new IllegalArgumentException("Final reading is required");
    }

    if (finalReading.compareTo(reading.getInitialReading()) < 0) {
      throw new IllegalArgumentException("Final reading cannot be lower than initial reading");
    }

    reading.setFinalReading(finalReading);
    reading.setFinalConfidenceScore(finalConfidenceScore);

    // Wartości się przeliczają na poziomie bazy

    // Tu warto dodać sprawdzenie finalConfidenceScore i w zależności od tego ustawić
    // setReadingStatus()

    reading.setReadingStatus(PENDING);

    mediaReadingRepository.save(reading);

    UtilityType utilityType = reading.getUtilityType();
    BigDecimal unitPrice = reading.getUnitPrice();
    BigDecimal consumptionDifference = finalReading.subtract(reading.getInitialReading());

    // Tworzy się jeszcze nie opłacone SettlementItem -> dla klienta powinien się pojawić przycisk
    settlementService.addUtilitySettlementItem(
        settlementId,
        unitId,
        mapUtilityType(utilityType),
        descriptionFor(utilityType),
        consumptionDifference,
        unitPrice);
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
