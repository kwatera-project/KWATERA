package io.github.kwatera_project.kwatera.billing_service.controller;

import io.github.kwatera_project.kwatera.billing_service.dto.MediaReadingStatusDto;
import io.github.kwatera_project.kwatera.billing_service.dto.MediaReadingUploadAttemptDto;
import io.github.kwatera_project.kwatera.billing_service.dto.MeterReadingResponseDto;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingType;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import io.github.kwatera_project.kwatera.billing_service.service.MediaReadingAccessService;
import io.github.kwatera_project.kwatera.billing_service.service.MediaReadingService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/billing/media-readings")
@RequiredArgsConstructor
public class MediaReadingController {

  private final MediaReadingService mediaReadingService;
  private final MediaReadingAccessService mediaReadingAccessService;

  private static final String AUTHORIZATION_HEADER = "Authorization";

  @GetMapping("/{settlementId}")
  @PreAuthorize("hasAnyAuthority('ROLE_GUEST', 'ROLE_OWNER', 'ROLE_ADMIN')")
  public ResponseEntity<List<MediaReadingStatusDto>> getMediaReadings(
      @PathVariable("settlementId") UUID settlementId,
      Authentication authentication,
      HttpServletRequest request) {

    mediaReadingAccessService.validateReadAccess(
        settlementId, authentication, request.getHeader(AUTHORIZATION_HEADER));
    return ResponseEntity.ok(mediaReadingService.getMediaReadings(settlementId));
  }

  @PostMapping("/{settlementId}/upload-initial")
  @PreAuthorize("hasAuthority('ROLE_GUEST')")
  public ResponseEntity<MeterReadingResponseDto> uploadInitialReading(
      @PathVariable("settlementId") UUID settlementId,
      @RequestParam("unitId") UUID unitId,
      @RequestParam("utilityType") UtilityType utilityType,
      @RequestParam("file") MultipartFile file,
      Authentication authentication,
      HttpServletRequest request)
      throws IOException {

    mediaReadingAccessService.validateGuestAccess(
        settlementId, authentication, request.getHeader(AUTHORIZATION_HEADER));
    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(settlementId, unitId, utilityType, file);

    return ResponseEntity.ok(MeterReadingResponseDto.from(status));
  }

  @PostMapping("/{settlementId}/upload-final")
  @PreAuthorize("hasAuthority('ROLE_GUEST')")
  public ResponseEntity<MeterReadingResponseDto> uploadFinalReading(
      @PathVariable("settlementId") UUID settlementId,
      @RequestParam("unitId") UUID unitId,
      @RequestParam("utilityType") UtilityType utilityType,
      @RequestParam("file") MultipartFile file,
      Authentication authentication,
      HttpServletRequest request)
      throws IOException {

    mediaReadingAccessService.validateGuestAccess(
        settlementId, authentication, request.getHeader(AUTHORIZATION_HEADER));
    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(settlementId, unitId, utilityType, file);

    return ResponseEntity.ok(MeterReadingResponseDto.from(status));
  }

  @PostMapping("/{settlementId}/approve")
  @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_ADMIN')")
  public ResponseEntity<Void> manuallyApproveReading(
      @PathVariable("settlementId") UUID settlementId,
      @RequestParam("unitId") UUID unitId,
      @RequestParam("utilityType") UtilityType utilityType,
      @RequestParam("correctedReading") BigDecimal correctedReading,
      @RequestParam("readingType") ReadingType readingType,
      Authentication authentication,
      HttpServletRequest request) {

    mediaReadingAccessService.validateReviewerAccess(
        settlementId, authentication, request.getHeader(AUTHORIZATION_HEADER));
    mediaReadingService.manuallyApproveReading(
        settlementId, unitId, utilityType, correctedReading, readingType);

    return ResponseEntity.ok().build();
  }

  @GetMapping("/{settlementId}/attempts")
  @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_ADMIN')")
  public ResponseEntity<List<MediaReadingUploadAttemptDto>> getUploadAttempts(
      @PathVariable("settlementId") UUID settlementId,
      @RequestParam("utilityType") UtilityType utilityType,
      Authentication authentication,
      HttpServletRequest request) {

    mediaReadingAccessService.validateReviewerAccess(
        settlementId, authentication, request.getHeader(AUTHORIZATION_HEADER));
    return ResponseEntity.ok(mediaReadingService.getUploadAttempts(settlementId, utilityType));
  }
}
