package io.github.kwatera_project.kwatera.billing_service.controller;

import io.github.kwatera_project.kwatera.billing_service.dto.MediaReadingStatusDto;
import io.github.kwatera_project.kwatera.billing_service.dto.MediaReadingUploadAttemptDto;
import io.github.kwatera_project.kwatera.billing_service.dto.MeterReadingResponseDto;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingType;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import io.github.kwatera_project.kwatera.billing_service.service.MediaReadingService;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
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

  @GetMapping("/{settlementId}")
  public ResponseEntity<List<MediaReadingStatusDto>> getMediaReadings(
      @PathVariable("settlementId") UUID settlementId) {

    return ResponseEntity.ok(mediaReadingService.getMediaReadings(settlementId));
  }

  @PostMapping("/{settlementId}/upload-initial")
  public ResponseEntity<MeterReadingResponseDto> uploadInitialReading(
      @PathVariable("settlementId") UUID settlementId,
      @RequestParam("unitId") UUID unitId,
      @RequestParam("utilityType") UtilityType utilityType,
      @RequestParam("file") MultipartFile file)
      throws IOException {

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(settlementId, unitId, utilityType, file);

    return ResponseEntity.ok(MeterReadingResponseDto.from(status));
  }

  @PostMapping("/{settlementId}/upload-final")
  public ResponseEntity<MeterReadingResponseDto> uploadFinalReading(
      @PathVariable("settlementId") UUID settlementId,
      @RequestParam("unitId") UUID unitId,
      @RequestParam("utilityType") UtilityType utilityType,
      @RequestParam("file") MultipartFile file)
      throws IOException {

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(settlementId, unitId, utilityType, file);

    return ResponseEntity.ok(MeterReadingResponseDto.from(status));
  }

  @PostMapping("/{settlementId}/approve")
  public ResponseEntity<Void> manuallyApproveReading(
      @PathVariable("settlementId") UUID settlementId,
      @RequestParam("unitId") UUID unitId,
      @RequestParam("utilityType") UtilityType utilityType,
      @RequestParam("correctedReading") BigDecimal correctedReading,
      @RequestParam("readingType") ReadingType readingType) {

    mediaReadingService.manuallyApproveReading(
        settlementId, unitId, utilityType, correctedReading, readingType);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/{settlementId}/attempts")
  public ResponseEntity<List<MediaReadingUploadAttemptDto>> getUploadAttempts(
      @PathVariable("settlementId") UUID settlementId,
      @RequestParam("utilityType") UtilityType utilityType) {

    return ResponseEntity.ok(mediaReadingService.getUploadAttempts(settlementId, utilityType));
  }
}
