package io.github.kwatera_project.kwatera.billing_service.service;

import io.github.kwatera_project.kwatera.billing_service.client.OcrClient;
import io.github.kwatera_project.kwatera.billing_service.dto.OcrResponseDto;
import io.github.kwatera_project.kwatera.billing_service.model.*;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingUploadAttemptRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
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

  @Value("${ocr.confidence-threshold}")
  private BigDecimal ocrConfidenceThreshold;

  @Transactional
  public void createMediaReading(UUID settlementId, UtilityType utilityType, BigDecimal unitPrice) {
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(utilityType);
    reading.setUnitPrice(unitPrice);
    mediaReadingRepository.save(reading);
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

    OcrResponseDto ocrResponse = ocrClient.readMeter(file);

    if (ocrResponse == null || ocrResponse.readingValue() == null) {
      throw new IllegalArgumentException("OCR could not read meter");
    }
    BigDecimal parsedReading;
    try {
      parsedReading = new BigDecimal(ocrResponse.readingValue().trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "OCR returned an invalid numeric value: " + ocrResponse.readingValue());
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
      UUID settlementId,
      UUID unitId,
      UtilityType utilityType,
      BigDecimal unitPrice,
      MultipartFile file)
      throws IOException {
    MediaReading reading =
        mediaReadingRepository
            .findBySettlementIdAndUtilityType(settlementId, utilityType)
            .orElseGet(
                () -> {
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

    OcrResponseDto ocrResponse = ocrClient.readMeter(file);

    if (ocrResponse == null || ocrResponse.readingValue() == null) {
      throw new IllegalArgumentException("OCR could not read meter");
    }

    BigDecimal parsedReading;
    try {
      parsedReading = new BigDecimal(ocrResponse.readingValue().trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "OCR returned an invalid numeric value: " + ocrResponse.readingValue());
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
      if (correctedReading.compareTo(reading.getInitialReading()) < 0) {
        throw new IllegalArgumentException(
            "Corrected reading cannot be lower than initial reading");
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
    if (finalReading.compareTo(reading.getInitialReading()) < 0) {
      throw new IllegalArgumentException("Final reading cannot be lower than initial reading");
    }

    boolean isRetry = reading.getFinalReadingStatus() == ReadingStatus.REQUEST_REUPLOAD;
    ReadingStatus status = determineReadingStatus(finalConfidenceScore, isRetry);
    reading.setFinalReadingStatus(status);

    if (status == ReadingStatus.AUTO_APPROVED) {
      reading.setFinalReading(finalReading);
      reading.setFinalConfidenceScore(finalConfidenceScore);
      reading.setFinalReadingSource(ReadingSource.OCR);
    }
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

  private String descriptionFor(UtilityType utilityType) {
    return switch (utilityType) {
      case WATER -> "Water usage";
      case ELECTRICITY -> "Electricity usage";
    };
  }
}
