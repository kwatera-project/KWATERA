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
import io.github.kwatera_project.kwatera.billing_service.service.MediaReadingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaReadingControllerTest {

  @Mock private MediaReadingService mediaReadingService;
  @Mock private MultipartFile multipartFile;

  @InjectMocks private MediaReadingController mediaReadingController;

  @Test
  void shouldGetMediaReadings() {
    UUID settlementId = UUID.randomUUID();

    when(mediaReadingService.getMediaReadings(settlementId)).thenReturn(List.of());

    ResponseEntity<List<MediaReadingStatusDto>> response =
        mediaReadingController.getMediaReadings(settlementId);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());
    verify(mediaReadingService).getMediaReadings(settlementId);
  }

  @Test
  void shouldUploadInitialReading() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(mediaReadingService.processInitialReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile))
        .thenReturn(ReadingStatus.AUTO_APPROVED);

    ResponseEntity<MeterReadingResponseDto> response =
        mediaReadingController.uploadInitialReading(
            settlementId, unitId, UtilityType.WATER, multipartFile);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(ReadingStatus.AUTO_APPROVED, response.getBody().status());

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

    ResponseEntity<MeterReadingResponseDto> response =
        mediaReadingController.uploadFinalReading(
            settlementId, unitId, UtilityType.WATER, multipartFile);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, response.getBody().status());

    verify(mediaReadingService)
        .processFinalReadingUpload(settlementId, unitId, UtilityType.WATER, multipartFile);
  }

  @Test
  void shouldManuallyApproveReading() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    ResponseEntity<Void> response =
        mediaReadingController.manuallyApproveReading(
            settlementId, unitId, UtilityType.WATER, new BigDecimal("123.45"), ReadingType.INITIAL);

    assertEquals(200, response.getStatusCode().value());

    verify(mediaReadingService)
        .manuallyApproveReading(
            settlementId, unitId, UtilityType.WATER, new BigDecimal("123.45"), ReadingType.INITIAL);
  }

  @Test
  void shouldGetUploadAttempts() {
    UUID settlementId = UUID.randomUUID();

    when(mediaReadingService.getUploadAttempts(settlementId, UtilityType.WATER))
        .thenReturn(List.of());

    ResponseEntity<List<MediaReadingUploadAttemptDto>> response =
        mediaReadingController.getUploadAttempts(settlementId, UtilityType.WATER);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());

    verify(mediaReadingService).getUploadAttempts(settlementId, UtilityType.WATER);
  }
}
