package io.github.kwatera_project.kwatera.billing_service.controller;

import io.github.kwatera_project.kwatera.billing_service.dto.MeterReadingResponseDto;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingType;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import io.github.kwatera_project.kwatera.billing_service.service.MediaReadingService;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/billing/media-readings")
@RequiredArgsConstructor
public class MediaReadingController {

  private final MediaReadingService mediaReadingService;

  @PostMapping("/{settlementId}/upload-initial")
  public ResponseEntity<MeterReadingResponseDto> uploadInitialReading(
      @PathVariable UUID settlementId,
      @RequestParam UUID unitId,
      @RequestParam UtilityType utilityType,
      @RequestParam BigDecimal unitPrice,
      @RequestParam MultipartFile file)
      throws IOException {

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, unitId, utilityType, unitPrice, file);

    return ResponseEntity.ok(MeterReadingResponseDto.from(status));
  }

  @PostMapping("/{settlementId}/upload-final")
  public ResponseEntity<MeterReadingResponseDto> uploadFinalReading(
      @PathVariable UUID settlementId,
      @RequestParam UUID unitId,
      @RequestParam UtilityType utilityType,
      @RequestParam MultipartFile file)
      throws IOException {

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(settlementId, unitId, utilityType, file);

    return ResponseEntity.ok(MeterReadingResponseDto.from(status));
  }

  @PostMapping("/{settlementId}/approve")
  public ResponseEntity<Void> manuallyApproveReading(
      @PathVariable UUID settlementId,
      @RequestParam UUID unitId,
      @RequestParam UtilityType utilityType,
      @RequestParam BigDecimal correctedReading,
      @RequestParam ReadingType readingType) {

    mediaReadingService.manuallyApproveReading(
        settlementId, unitId, utilityType, correctedReading, readingType);

    return ResponseEntity.ok().build();
  }
}
