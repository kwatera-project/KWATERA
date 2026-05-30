package io.github.kwatera_project.kwatera.billing_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.dto.MediaReadingStatusDto;
import io.github.kwatera_project.kwatera.billing_service.dto.MediaReadingUploadAttemptDto;
import io.github.kwatera_project.kwatera.billing_service.dto.MeterReadingResponseDto;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingType;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import io.github.kwatera_project.kwatera.billing_service.service.MediaReadingAccessService;
import io.github.kwatera_project.kwatera.billing_service.service.MediaReadingService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaReadingControllerTest {

  @Mock private MediaReadingService mediaReadingService;
  @Mock private MediaReadingAccessService mediaReadingAccessService;
  @Mock private MultipartFile multipartFile;
  @Mock private Authentication authentication;
  @Mock private HttpServletRequest request;

  @InjectMocks private MediaReadingController mediaReadingController;

  private static final String TOKEN = "Bearer token";

  @Test
  void shouldGetMediaReadings() {
    UUID settlementId = UUID.randomUUID();

    when(mediaReadingService.getMediaReadings(settlementId)).thenReturn(List.of());
    when(request.getHeader("Authorization")).thenReturn(TOKEN);

    ResponseEntity<List<MediaReadingStatusDto>> response =
        mediaReadingController.getMediaReadings(settlementId, authentication, request);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());
    verify(mediaReadingAccessService).validateReadAccess(settlementId, authentication, TOKEN);
    verify(mediaReadingService).getMediaReadings(settlementId);
  }

  @Test
  void shouldUploadInitialReading() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(mediaReadingService.processInitialReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile))
        .thenReturn(ReadingStatus.AUTO_APPROVED);
    when(request.getHeader("Authorization")).thenReturn(TOKEN);

    ResponseEntity<MeterReadingResponseDto> response =
        mediaReadingController.uploadInitialReading(
            settlementId, unitId, UtilityType.WATER, multipartFile, authentication, request);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(ReadingStatus.AUTO_APPROVED, response.getBody().status());

    verify(mediaReadingAccessService)
        .validateGuestAccess(settlementId, unitId, authentication, TOKEN);
    verify(mediaReadingService)
        .processInitialReadingUpload(settlementId, unitId, UtilityType.WATER, multipartFile);
  }

  @Test
  void shouldUploadFinalReading() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(mediaReadingService.processFinalReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile))
        .thenReturn(ReadingStatus.REQUEST_REUPLOAD);
    when(request.getHeader("Authorization")).thenReturn(TOKEN);

    ResponseEntity<MeterReadingResponseDto> response =
        mediaReadingController.uploadFinalReading(
            settlementId, unitId, UtilityType.WATER, multipartFile, authentication, request);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, response.getBody().status());

    verify(mediaReadingAccessService)
        .validateGuestAccess(settlementId, unitId, authentication, TOKEN);
    verify(mediaReadingService)
        .processFinalReadingUpload(settlementId, unitId, UtilityType.WATER, multipartFile);
  }

  @Test
  void shouldManuallyApproveReading() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    when(request.getHeader("Authorization")).thenReturn(TOKEN);

    ResponseEntity<Void> response =
        mediaReadingController.manuallyApproveReading(
            settlementId,
            unitId,
            UtilityType.WATER,
            new BigDecimal("123.45"),
            ReadingType.INITIAL,
            authentication,
            request);

    assertEquals(200, response.getStatusCode().value());

    verify(mediaReadingAccessService)
        .validateReviewerAccess(settlementId, unitId, authentication, TOKEN);
    verify(mediaReadingService)
        .manuallyApproveReading(
            settlementId, unitId, UtilityType.WATER, new BigDecimal("123.45"), ReadingType.INITIAL);
  }

  @Test
  void shouldGetUploadAttempts() {
    UUID settlementId = UUID.randomUUID();

    when(mediaReadingService.getUploadAttempts(settlementId, UtilityType.WATER))
        .thenReturn(List.of());
    when(request.getHeader("Authorization")).thenReturn(TOKEN);

    ResponseEntity<List<MediaReadingUploadAttemptDto>> response =
        mediaReadingController.getUploadAttempts(
            settlementId, UtilityType.WATER, authentication, request);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());

    verify(mediaReadingAccessService).validateReviewerAccess(settlementId, authentication, TOKEN);
    verify(mediaReadingService).getUploadAttempts(settlementId, UtilityType.WATER);
  }
}
