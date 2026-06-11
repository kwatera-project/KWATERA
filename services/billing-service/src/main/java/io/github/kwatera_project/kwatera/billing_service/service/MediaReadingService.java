package io.github.kwatera_project.kwatera.billing_service.service;

import io.github.kwatera_project.kwatera.billing_service.client.OcrClient;
import io.github.kwatera_project.kwatera.billing_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.billing_service.client.ReservationClient;
import io.github.kwatera_project.kwatera.billing_service.dto.*;
import io.github.kwatera_project.kwatera.billing_service.model.*;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingUploadAttemptRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MediaReadingService {

  private final MediaReadingRepository mediaReadingRepository;
  private final SettlementService settlementService;
  private final OcrClient ocrClient;
  private final MediaReadingUploadAttemptRepository uploadAttemptRepository;
  private final PropertyClient propertyClient;
  private final SettlementRepository settlementRepository;
  private final ReservationClient reservationClient;

  @Value("${ocr.confidence-threshold}")
  private BigDecimal ocrConfidenceThreshold;

  public List<MediaReadingStatusDto> getMediaReadings(UUID settlementId) {
    return mediaReadingRepository.findBySettlementId(settlementId).stream()
        .map(MediaReadingStatusDto::from)
        .toList();
  }

  @Transactional
  public ReadingStatus processFinalReadingUpload(
      UUID settlementId, UUID unitId, UtilityType utilityType, MultipartFile file)
      throws IOException {

    MediaReading reading =
        mediaReadingRepository
            .findBySettlementIdAndUtilityType(settlementId, utilityType)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Media Reading not found for settlementId: " + settlementId));

    if (reading.getFinalReadingStatus() != ReadingStatus.PENDING
        && reading.getFinalReadingStatus() != ReadingStatus.REQUEST_REUPLOAD) {
      throw new IllegalStateException(
          "Upload not allowed in current status: " + reading.getFinalReadingStatus());
    }
    if (reading.getInitialReadingStatus() != ReadingStatus.AUTO_APPROVED
        && reading.getInitialReadingStatus() != ReadingStatus.MANUALLY_APPROVED) {
      throw new IllegalStateException(
          "Cannot upload final reading before initial reading is approved");
    }

    OcrResponseDto ocrResponse;
    try {
      ocrResponse = ocrClient.readMeter(file);
    } catch (RuntimeException e) {
      return handleFailedOcrAttempt(reading, file.getBytes(), ReadingType.FINAL);
    }

    if (ocrResponse == null
        || ocrResponse.readingValue() == null
        || ocrResponse.confidence() == null) {
      return handleFailedOcrAttempt(reading, file.getBytes(), ReadingType.FINAL);
    }

    BigDecimal parsedReading;
    try {
      parsedReading = new BigDecimal(ocrResponse.readingValue().trim());
    } catch (NumberFormatException e) {
      return handleFailedOcrAttempt(reading, file.getBytes(), ReadingType.FINAL);
    }

    return addFinalMediaReading(
        settlementId,
        unitId,
        utilityType,
        parsedReading,
        ocrResponse.confidence(),
        ocrResponse.readingValue(),
        file.getBytes(),
        reading);
  }

  @Transactional
  public ReadingStatus processInitialReadingUpload(
      UUID settlementId, UUID unitId, UtilityType utilityType, MultipartFile file)
      throws IOException {
    MediaReading reading =
        mediaReadingRepository
            .findBySettlementIdAndUtilityType(settlementId, utilityType)
            .orElseGet(
                () -> {
                  BigDecimal unitPrice = resolveUnitPrice(unitId, utilityType);
                  MediaReading r = new MediaReading();
                  r.setSettlementId(settlementId);
                  r.setUtilityType(utilityType);
                  r.setUnitPrice(unitPrice);
                  return mediaReadingRepository.save(r);
                });

    if (reading.getInitialReadingStatus() != ReadingStatus.PENDING
        && reading.getInitialReadingStatus() != ReadingStatus.REQUEST_REUPLOAD) {
      throw new IllegalStateException(
          "Upload not allowed in current status: " + reading.getInitialReadingStatus());
    }

    OcrResponseDto ocrResponse;
    try {
      ocrResponse = ocrClient.readMeter(file);
    } catch (RuntimeException e) {
      return handleFailedOcrAttempt(reading, file.getBytes(), ReadingType.INITIAL);
    }

    if (ocrResponse == null
        || ocrResponse.readingValue() == null
        || ocrResponse.confidence() == null) {
      return handleFailedOcrAttempt(reading, file.getBytes(), ReadingType.INITIAL);
    }

    BigDecimal parsedReading;
    try {
      parsedReading = new BigDecimal(ocrResponse.readingValue().trim());
    } catch (NumberFormatException e) {
      return handleFailedOcrAttempt(reading, file.getBytes(), ReadingType.INITIAL);
    }

    return addInitialMediaReading(
        parsedReading,
        ocrResponse.confidence(),
        ocrResponse.readingValue(),
        file.getBytes(),
        reading);
  }

  @Transactional
  public void manuallyApproveReading(
      UUID settlementId,
      UUID unitId,
      UtilityType utilityType,
      BigDecimal correctedReading,
      ReadingType readingType) {

    MediaReading reading =
        mediaReadingRepository
            .findBySettlementIdAndUtilityType(settlementId, utilityType)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Media Reading not found for settlementId: " + settlementId));

    if (readingType == ReadingType.INITIAL) {
      if (reading.getInitialReadingStatus() != ReadingStatus.REQUEST_MANUAL_REVIEW) {
        throw new IllegalStateException(
            "Manual approval not allowed in current status: " + reading.getInitialReadingStatus());
      }
      reading.setInitialReading(correctedReading);
      reading.setInitialReadingSource(ReadingSource.MANUAL);
      reading.setInitialReadingStatus(ReadingStatus.MANUALLY_APPROVED);
      mediaReadingRepository.save(reading);

    } else {
      if (reading.getFinalReadingStatus() != ReadingStatus.REQUEST_MANUAL_REVIEW) {
        throw new IllegalStateException(
            "Manual approval not allowed in current status: " + reading.getFinalReadingStatus());
      }
      if (reading.getInitialReading() == null) {
        throw new IllegalStateException("Initial reading must be approved before final approval");
      }
      if (correctedReading.compareTo(reading.getInitialReading()) <= 0) {
        throw new IllegalArgumentException(
            "Corrected final reading must be greater than initial reading");
      }
      reading.setFinalReading(correctedReading);
      reading.setFinalReadingSource(ReadingSource.MANUAL);
      reading.setFinalReadingStatus(ReadingStatus.MANUALLY_APPROVED);
      mediaReadingRepository.save(reading);

      BigDecimal consumptionDifference = correctedReading.subtract(reading.getInitialReading());
      settlementService.addUtilitySettlementItem(
          settlementId,
          unitId,
          mapUtilityType(utilityType),
          descriptionFor(utilityType),
          consumptionDifference,
          reading.getUnitPrice());
    }
  }

  private ReadingStatus addFinalMediaReading(
      UUID settlementId,
      UUID unitId,
      UtilityType utilityType,
      BigDecimal finalReading,
      BigDecimal finalConfidenceScore,
      String ocrValue,
      byte[] imageBytes,
      MediaReading reading) {

    if (reading.getInitialReading() == null) {
      throw new IllegalStateException("Initial reading must be approved before final reading");
    }

    boolean isRetry = reading.getFinalReadingStatus() == ReadingStatus.REQUEST_REUPLOAD;

    if (finalReading.compareTo(reading.getInitialReading()) <= 0) {
      ReadingStatus status =
          isRetry ? ReadingStatus.REQUEST_MANUAL_REVIEW : ReadingStatus.REQUEST_REUPLOAD;

      reading.setFinalReadingStatus(status);
      mediaReadingRepository.save(reading);

      saveUploadAttempt(
          reading.getId(), imageBytes, ocrValue, finalConfidenceScore, status, ReadingType.FINAL);

      return status;
    }
    ReadingStatus status = determineReadingStatus(finalConfidenceScore, isRetry);

    if (status == ReadingStatus.AUTO_APPROVED) {
      BigDecimal consumptionDifference = finalReading.subtract(reading.getInitialReading());

      Settlement settlement = settlementRepository.findById(settlementId).orElseThrow();
      ReservationDto reservation = reservationClient.getReservation(settlement.getReservationId());
      UnitDto unit = propertyClient.getUnit(unitId);
      int capacity = (unit != null && unit.getCapacity() != null) ? unit.getCapacity() : 1;

      if (utilityType == UtilityType.WATER
          && reservation != null
          && reservation.getStartDate() != null
          && reservation.getEndDate() != null
          && isConsumptionSuspicious(
              consumptionDifference,
              reservation.getStartDate(),
              reservation.getEndDate(),
              capacity)) {
        status = ReadingStatus.REQUEST_MANUAL_REVIEW;
        reading.setFinalReadingStatus(status);
        mediaReadingRepository.save(reading);
        saveUploadAttempt(
            reading.getId(), imageBytes, ocrValue, finalConfidenceScore, status, ReadingType.FINAL);
        return status;
      }

      reading.setFinalReading(finalReading);
      reading.setFinalConfidenceScore(finalConfidenceScore);
      reading.setFinalReadingSource(ReadingSource.OCR);
    }

    reading.setFinalReadingStatus(status);
    mediaReadingRepository.save(reading);
    saveUploadAttempt(
        reading.getId(), imageBytes, ocrValue, finalConfidenceScore, status, ReadingType.FINAL);

    if (status == ReadingStatus.AUTO_APPROVED) {
      BigDecimal consumptionDifference = finalReading.subtract(reading.getInitialReading());
      settlementService.addUtilitySettlementItem(
          settlementId,
          unitId,
          mapUtilityType(utilityType),
          descriptionFor(utilityType),
          consumptionDifference,
          reading.getUnitPrice());
    }

    return status;
  }

  public List<MediaReadingUploadAttemptDto> getUploadAttempts(
      UUID settlementId, UtilityType utilityType) {
    return mediaReadingRepository
        .findBySettlementIdAndUtilityType(settlementId, utilityType)
        .map(
            reading ->
                uploadAttemptRepository
                    .findByMediaReadingIdOrderByAttemptedAtAsc(reading.getId())
                    .stream()
                    .map(MediaReadingUploadAttemptDto::from)
                    .toList())
        .orElse(List.of());
  }

  private ReadingStatus handleFailedOcrAttempt(
      MediaReading reading, byte[] imageBytes, ReadingType readingType) {
    boolean isRetry =
        readingType == ReadingType.INITIAL
            ? reading.getInitialReadingStatus() == ReadingStatus.REQUEST_REUPLOAD
            : reading.getFinalReadingStatus() == ReadingStatus.REQUEST_REUPLOAD;

    ReadingStatus status =
        isRetry ? ReadingStatus.REQUEST_MANUAL_REVIEW : ReadingStatus.REQUEST_REUPLOAD;

    if (readingType == ReadingType.INITIAL) {
      reading.setInitialReadingStatus(status);
    } else {
      reading.setFinalReadingStatus(status);
    }

    mediaReadingRepository.save(reading);

    saveUploadAttempt(reading.getId(), imageBytes, null, BigDecimal.ZERO, status, readingType);

    return status;
  }

  private void saveUploadAttempt(
      UUID mediaReadingId,
      byte[] imageBytes,
      String ocrValue,
      BigDecimal confidenceScore,
      ReadingStatus status,
      ReadingType readingType) {

    MediaReadingUploadAttempt attempt = new MediaReadingUploadAttempt();
    attempt.setMediaReadingId(mediaReadingId);
    attempt.setMeterImage(imageBytes);
    attempt.setOcrValue(ocrValue);
    attempt.setConfidenceScore(confidenceScore);
    attempt.setStatus(status);
    attempt.setReadingType(readingType);
    uploadAttemptRepository.save(attempt);
  }

  private ReadingStatus determineReadingStatus(BigDecimal confidenceScore, boolean isRetry) {
    if (confidenceScore.compareTo(ocrConfidenceThreshold) >= 0) {
      return ReadingStatus.AUTO_APPROVED;
    }
    if (isRetry) {
      return ReadingStatus.REQUEST_MANUAL_REVIEW;
    }
    return ReadingStatus.REQUEST_REUPLOAD;
  }

  private ReadingStatus addInitialMediaReading(
      BigDecimal initialReading,
      BigDecimal confidenceScore,
      String ocrValue,
      byte[] imageBytes,
      MediaReading reading) {
    boolean isRetry = reading.getInitialReadingStatus() == ReadingStatus.REQUEST_REUPLOAD;
    ReadingStatus status = determineReadingStatus(confidenceScore, isRetry);

    reading.setInitialReadingStatus(status);
    if (status == ReadingStatus.AUTO_APPROVED) {
      reading.setInitialReading(initialReading);
      reading.setInitialConfidenceScore(confidenceScore);
      reading.setInitialReadingSource(ReadingSource.OCR);
    }
    mediaReadingRepository.save(reading);

    saveUploadAttempt(
        reading.getId(), imageBytes, ocrValue, confidenceScore, status, ReadingType.INITIAL);

    return status;
  }

  private SettlementItemType mapUtilityType(UtilityType utilityType) {
    return switch (utilityType) {
      case WATER -> SettlementItemType.WATER;
      case ELECTRICITY -> SettlementItemType.ELECTRICITY;
    };
  }

  private BigDecimal resolveUnitPrice(UUID unitId, UtilityType utilityType) {
    SettlementItemType settlementItemType = mapUtilityType(utilityType);

    return propertyClient.getUnitSettlementItems(unitId).stream()
        .filter(item -> item.settlementItemType() == settlementItemType)
        .map(UnitSettlementItemDto::pricePerUnit)
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No tariff configured for unit " + unitId + " and utility " + utilityType));
  }

  private boolean isConsumptionSuspicious(
      BigDecimal consumptionDifference, LocalDate startDate, LocalDate endDate, int capacity) {

    long stayDays = ChronoUnit.DAYS.between(startDate, endDate);
    stayDays = Math.max(stayDays, 1);

    BigDecimal expectedUsage =
        BigDecimal.valueOf(capacity)
            .multiply(BigDecimal.valueOf(stayDays))
            .multiply(new BigDecimal("0.1"));

    BigDecimal minUsage = expectedUsage.multiply(new BigDecimal("0.2"));
    BigDecimal maxUsage = expectedUsage.multiply(new BigDecimal("3.0"));

    return consumptionDifference.compareTo(minUsage) < 0
        || consumptionDifference.compareTo(maxUsage) > 0;
  }

  private String descriptionFor(UtilityType utilityType) {
    return switch (utilityType) {
      case WATER -> "Water usage";
      case ELECTRICITY -> "Electricity usage";
    };
  }
}
