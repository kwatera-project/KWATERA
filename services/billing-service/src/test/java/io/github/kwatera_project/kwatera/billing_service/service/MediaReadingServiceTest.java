package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.billing_service.client.OcrClient;
import io.github.kwatera_project.kwatera.billing_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.billing_service.dto.OcrResponseDto;
import io.github.kwatera_project.kwatera.billing_service.dto.UnitSettlementItemDto;
import io.github.kwatera_project.kwatera.billing_service.model.*;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingUploadAttemptRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaReadingServiceTest {

  @Mock private MediaReadingRepository mediaReadingRepository;
  @Mock private SettlementService settlementService;
  @Mock private OcrClient ocrClient;
  @Mock private MediaReadingUploadAttemptRepository uploadAttemptRepository;
  @Mock private PropertyClient propertyClient;
  @Mock private MultipartFile multipartFile;

  @Mock
  private io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository
      settlementRepository;

  @Mock
  private io.github.kwatera_project.kwatera.billing_service.client.ReservationClient
      reservationClient;

  @InjectMocks private MediaReadingService mediaReadingService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
        mediaReadingService, "ocrConfidenceThreshold", new BigDecimal("0.70"));
  }

  private UnitSettlementItemDto tariff(
      UUID unitId, SettlementItemType type, BigDecimal pricePerUnit) {
    return new UnitSettlementItemDto(
        UUID.randomUUID(), unitId, type, pricePerUnit, MeasurementUnit.M3, BillingType.PER_USAGE);
  }

  @Test
  void shouldAutoApproveInitialReadingWhenConfidenceIsHigh() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("120.50", new BigDecimal("0.95")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);
    assertEquals(ReadingStatus.AUTO_APPROVED, status);
    assertEquals(new BigDecimal("120.50"), reading.getInitialReading());
    assertEquals(ReadingSource.OCR, reading.getInitialReadingSource());
  }

  @Test
  void shouldRequestReuploadForInitialWhenConfidenceIsLow() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("120.50", new BigDecimal("0.50")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertNull(reading.getInitialReading());
  }

  @Test
  void shouldRequestManualReviewForInitialOnSecondFailure() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);
    reading.setInitialReadingStatus(ReadingStatus.REQUEST_REUPLOAD);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("120.50", new BigDecimal("0.50")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_MANUAL_REVIEW, status);
    assertNull(reading.getInitialReading());
  }

  @Test
  void shouldRejectInitialUploadWhenStatusIsNotAllowed() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));

    assertThrows(
        IllegalStateException.class,
        () ->
            mediaReadingService.processInitialReadingUpload(
                settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile));

    verifyNoInteractions(ocrClient);
    verifyNoInteractions(settlementService);
  }

  @Test
  void shouldCreateNewReadingIfNotExistsOnInitialUpload() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.empty());
    when(propertyClient.getUnitSettlementItems(unitId))
        .thenReturn(List.of(tariff(unitId, SettlementItemType.WATER, new BigDecimal("18.50"))));
    when(mediaReadingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("100.00", new BigDecimal("0.95")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.AUTO_APPROVED, status);
  }

  @Test
  void shouldSnapshotUnitPriceFromPropertyServiceTariffOnInitialUpload() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.empty());
    when(propertyClient.getUnitSettlementItems(unitId))
        .thenReturn(List.of(tariff(unitId, SettlementItemType.WATER, new BigDecimal("18.50"))));
    when(mediaReadingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("100.00", new BigDecimal("0.95")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    mediaReadingService.processInitialReadingUpload(
        settlementId, unitId, UtilityType.WATER, multipartFile);

    verify(mediaReadingRepository, atLeastOnce())
        .save(
            argThat(
                reading ->
                    UtilityType.WATER == reading.getUtilityType()
                        && new BigDecimal("18.50").compareTo(reading.getUnitPrice()) == 0));
  }

  @Test
  void shouldRejectInitialUploadWhenWaterTariffIsMissing() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.empty());
    when(propertyClient.getUnitSettlementItems(unitId))
        .thenReturn(
            List.of(tariff(unitId, SettlementItemType.ELECTRICITY, new BigDecimal("0.90"))));

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                mediaReadingService.processInitialReadingUpload(
                    settlementId, unitId, UtilityType.WATER, multipartFile));

    assertTrue(exception.getMessage().contains("No tariff configured"));
    verify(mediaReadingRepository, never()).save(any(MediaReading.class));
    verifyNoInteractions(ocrClient);
  }

  @Test
  void shouldAutoApproveFinalReadingAndCreateSettlementItem() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);
    reading.setUnitPrice(new BigDecimal("5.00"));

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("150", new BigDecimal("0.99")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);
    Settlement settlement = new Settlement();
    settlement.setReservationId(UUID.randomUUID());
    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
    when(reservationClient.getReservation(any())).thenReturn(null);
    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.AUTO_APPROVED, status);
    assertEquals(new BigDecimal("150"), reading.getFinalReading());
    verify(settlementService)
        .addUtilitySettlementItem(
            eq(settlementId),
            eq(unitId),
            eq(SettlementItemType.WATER),
            eq("Water usage"),
            eq(new BigDecimal("50")),
            eq(new BigDecimal("5.00")));
  }

  @Test
  void shouldAutoApproveElectricityFinalReadingAndCreateSettlementItem() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("1000"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);
    reading.setUnitPrice(new BigDecimal("0.90"));

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(
            settlementId, UtilityType.ELECTRICITY))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("1250", new BigDecimal("0.99")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    // DODAJ:
    Settlement settlement = new Settlement();
    settlement.setReservationId(UUID.randomUUID());
    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
    when(reservationClient.getReservation(any())).thenReturn(null);

    mediaReadingService.processFinalReadingUpload(
        settlementId, unitId, UtilityType.ELECTRICITY, multipartFile);

    verify(settlementService)
        .addUtilitySettlementItem(
            eq(settlementId),
            eq(unitId),
            eq(SettlementItemType.ELECTRICITY),
            eq("Electricity usage"),
            eq(new BigDecimal("250")),
            eq(new BigDecimal("0.90")));
  }

  @Test
  void shouldRequestReuploadForFinalWhenConfidenceIsLow() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);
    reading.setUnitPrice(new BigDecimal("5.00"));

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("150", new BigDecimal("0.50")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertNull(reading.getFinalReading());
    verifyNoInteractions(settlementService);
  }

  @Test
  void shouldRequestManualReviewForFinalOnSecondFailure() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.REQUEST_REUPLOAD);
    reading.setUnitPrice(new BigDecimal("5.00"));

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("150", new BigDecimal("0.50")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_MANUAL_REVIEW, status);
    assertNull(reading.getFinalReading());
    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldRequestReuploadWhenFinalReadingIsLowerThanInitial() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("200"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("100", new BigDecimal("0.99")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getFinalReadingStatus());
    assertNull(reading.getFinalReading());
    verifyNoInteractions(settlementService);
  }

  @Test
  void shouldRejectFinalUploadWhenInitialNotApproved() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReadingStatus(ReadingStatus.REQUEST_REUPLOAD);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));

    assertThrows(
        IllegalStateException.class,
        () ->
            mediaReadingService.processFinalReadingUpload(
                settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile));

    verifyNoInteractions(ocrClient);
    verifyNoInteractions(settlementService);
  }

  @Test
  void shouldThrowWhenFinalReadingNotFound() {
    UUID settlementId = UUID.randomUUID();

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () ->
            mediaReadingService.processFinalReadingUpload(
                settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile));

    verifyNoInteractions(ocrClient);
    verifyNoInteractions(settlementService);
  }

  @Test
  void shouldManuallyApproveFinalReading() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setUnitPrice(new BigDecimal("5.00"));
    reading.setFinalReadingStatus(ReadingStatus.REQUEST_MANUAL_REVIEW);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));

    mediaReadingService.manuallyApproveReading(
        settlementId, unitId, UtilityType.WATER, new BigDecimal("140"), ReadingType.FINAL);

    assertEquals(new BigDecimal("140"), reading.getFinalReading());
    assertEquals(ReadingStatus.MANUALLY_APPROVED, reading.getFinalReadingStatus());
    assertEquals(ReadingSource.MANUAL, reading.getFinalReadingSource());
    verify(settlementService)
        .addUtilitySettlementItem(
            eq(settlementId),
            eq(unitId),
            eq(SettlementItemType.WATER),
            eq("Water usage"),
            eq(new BigDecimal("40")),
            eq(new BigDecimal("5.00")));
  }

  @Test
  void shouldManuallyApproveInitialReading() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReadingStatus(ReadingStatus.REQUEST_MANUAL_REVIEW);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));

    mediaReadingService.manuallyApproveReading(
        settlementId, unitId, UtilityType.WATER, new BigDecimal("100"), ReadingType.INITIAL);

    assertEquals(new BigDecimal("100"), reading.getInitialReading());
    assertEquals(ReadingStatus.MANUALLY_APPROVED, reading.getInitialReadingStatus());
    assertEquals(ReadingSource.MANUAL, reading.getInitialReadingSource());
    verifyNoInteractions(settlementService);
  }

  @Test
  void shouldRejectManualApprovalOfFinalWhenStatusIsWrong() {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setFinalReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));

    assertThrows(
        IllegalStateException.class,
        () ->
            mediaReadingService.manuallyApproveReading(
                settlementId,
                UUID.randomUUID(),
                UtilityType.WATER,
                new BigDecimal("140"),
                ReadingType.FINAL));
    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldRejectManualApprovalOfInitialWhenStatusIsWrong() {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));

    assertThrows(
        IllegalStateException.class,
        () ->
            mediaReadingService.manuallyApproveReading(
                settlementId,
                UUID.randomUUID(),
                UtilityType.WATER,
                new BigDecimal("100"),
                ReadingType.INITIAL));
  }

  @Test
  void shouldRejectManualFinalApprovalWhenCorrectedReadingIsLowerThanInitial() {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("200"));
    reading.setFinalReadingStatus(ReadingStatus.REQUEST_MANUAL_REVIEW);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            mediaReadingService.manuallyApproveReading(
                settlementId,
                UUID.randomUUID(),
                UtilityType.WATER,
                new BigDecimal("100"),
                ReadingType.FINAL));
    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldThrowWhenManualApprovalReadingNotFound() {
    UUID settlementId = UUID.randomUUID();

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () ->
            mediaReadingService.manuallyApproveReading(
                settlementId,
                UUID.randomUUID(),
                UtilityType.WATER,
                new BigDecimal("140"),
                ReadingType.FINAL));
    verifyNoInteractions(settlementService);
  }

  @Test
  void shouldRequestReuploadWhenInitialOcrClientFails() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);
    reading.setInitialReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile)).thenThrow(new RuntimeException("OCR failed"));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getInitialReadingStatus());
    assertNull(reading.getInitialReading());
  }

  @Test
  void shouldRequestManualReviewWhenInitialOcrClientFailsOnRetry() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);
    reading.setInitialReadingStatus(ReadingStatus.REQUEST_REUPLOAD);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile)).thenThrow(new RuntimeException("OCR failed"));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_MANUAL_REVIEW, status);
    assertEquals(ReadingStatus.REQUEST_MANUAL_REVIEW, reading.getInitialReadingStatus());
    assertNull(reading.getInitialReading());
  }

  @Test
  void shouldRequestReuploadWhenInitialOcrReturnsNull() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);
    reading.setInitialReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile)).thenReturn(null);
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getInitialReadingStatus());
  }

  @Test
  void shouldRequestReuploadWhenInitialOcrReturnsNullReadingValue() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);
    reading.setInitialReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto(null, new BigDecimal("0.95")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getInitialReadingStatus());
    assertNull(reading.getInitialReading());
  }

  @Test
  void shouldRequestReuploadWhenInitialOcrReturnsNullConfidence() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);
    reading.setInitialReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile)).thenReturn(new OcrResponseDto("120.50", null));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getInitialReadingStatus());
    assertNull(reading.getInitialReading());
  }

  @Test
  void shouldRequestReuploadWhenInitialOcrReturnsInvalidNumber() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setSettlementId(settlementId);
    reading.setUtilityType(UtilityType.WATER);
    reading.setInitialReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("not-a-number", new BigDecimal("0.95")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processInitialReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getInitialReadingStatus());
    assertNull(reading.getInitialReading());
  }

  @Test
  void shouldRequestManualReviewWhenFinalOcrClientFailsOnRetry() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.REQUEST_REUPLOAD);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile)).thenThrow(new RuntimeException("OCR failed"));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_MANUAL_REVIEW, status);
    assertEquals(ReadingStatus.REQUEST_MANUAL_REVIEW, reading.getFinalReadingStatus());
    assertNull(reading.getFinalReading());
    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldRequestReuploadWhenFinalOcrReturnsNullConfidence() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile)).thenReturn(new OcrResponseDto("150", null));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getFinalReadingStatus());
    assertNull(reading.getFinalReading());

    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  // Duplicate handling: approved final readings must not create another utility charge.
  @Test
  void shouldBlockDuplicateAutoApprovedFinalCharge() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReading(new BigDecimal("150"));
    reading.setFinalReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setUnitPrice(new BigDecimal("5.00"));

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));

    assertThrows(
        IllegalStateException.class,
        () ->
            mediaReadingService.processFinalReadingUpload(
                settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile));

    verifyNoInteractions(ocrClient);
    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldBlockDuplicateManuallyApprovedFinalCharge() {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setFinalReading(new BigDecimal("150"));
    reading.setFinalReadingStatus(ReadingStatus.MANUALLY_APPROVED);
    reading.setUnitPrice(new BigDecimal("5.00"));

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));

    assertThrows(
        IllegalStateException.class,
        () ->
            mediaReadingService.manuallyApproveReading(
                settlementId,
                UUID.randomUUID(),
                UtilityType.WATER,
                new BigDecimal("160"),
                ReadingType.FINAL));

    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  // Failure path: invalid OCR result must not change settlement.
  @Test
  void shouldNotUpdateSettlementForInvalidFinalOcrValue() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("bad-value", new BigDecimal("0.95")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getFinalReadingStatus());
    assertNull(reading.getFinalReading());

    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldNotUpdateSettlementWhenFinalOcrFails() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile)).thenThrow(new RuntimeException("OCR failed"));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getFinalReadingStatus());
    assertNull(reading.getFinalReading());

    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldNotUpdateSettlementWhenFinalOcrReturnsNull() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile)).thenReturn(null);
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getFinalReadingStatus());
    assertNull(reading.getFinalReading());

    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldNotUpdateSettlementWhenFinalOcrReturnsNullReadingValue() throws Exception {
    UUID settlementId = UUID.randomUUID();
    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto(null, new BigDecimal("0.95")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, UUID.randomUUID(), UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_REUPLOAD, status);
    assertEquals(ReadingStatus.REQUEST_REUPLOAD, reading.getFinalReadingStatus());
    assertNull(reading.getFinalReading());

    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldRequestManualReviewWhenWaterConsumptionIsSuspiciouslyHigh() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();

    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);
    reading.setUnitPrice(new BigDecimal("5.00"));

    Settlement settlement = new Settlement();
    settlement.setReservationId(reservationId);

    io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto reservation =
        new io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto();
    reservation.setStartDate(java.time.LocalDate.of(2026, 1, 1));
    reservation.setEndDate(java.time.LocalDate.of(2026, 1, 4)); // 3 days

    io.github.kwatera_project.kwatera.billing_service.dto.UnitDto unit =
        new io.github.kwatera_project.kwatera.billing_service.dto.UnitDto();
    unit.setCapacity(2); // 2 persons
    // expected = 2 * 3 * 0.1 = 0.6 m3, max = 0.6 * 3.0 = 1.8 m3
    // consumption = 150 - 100 = 50 m3 → SUSPICIOUS (50 > 1.8)

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("150", new BigDecimal("0.99")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);
    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
    when(reservationClient.getReservation(reservationId)).thenReturn(reservation);
    when(propertyClient.getUnit(unitId)).thenReturn(unit);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_MANUAL_REVIEW, status);
    assertNull(reading.getFinalReading());
    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldRequestManualReviewWhenWaterConsumptionIsSuspiciouslyLow() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();

    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100.00"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);
    reading.setUnitPrice(new BigDecimal("5.00"));

    Settlement settlement = new Settlement();
    settlement.setReservationId(reservationId);

    io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto reservation =
        new io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto();
    reservation.setStartDate(java.time.LocalDate.of(2026, 1, 1));
    reservation.setEndDate(java.time.LocalDate.of(2026, 1, 11)); // 10 days

    io.github.kwatera_project.kwatera.billing_service.dto.UnitDto unit =
        new io.github.kwatera_project.kwatera.billing_service.dto.UnitDto();
    unit.setCapacity(4); // 4 persons
    // expected = 4 * 10 * 0.1 = 4.0 m3, min = 4.0 * 0.2 = 0.8 m3
    // consumption = 100.01 - 100.00 = 0.01 m3 → SUSPICIOUS (0.01 < 0.8)

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("100.01", new BigDecimal("0.99")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);
    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
    when(reservationClient.getReservation(reservationId)).thenReturn(reservation);
    when(propertyClient.getUnit(unitId)).thenReturn(unit);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.REQUEST_MANUAL_REVIEW, status);
    assertNull(reading.getFinalReading());
    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldAutoApproveWhenWaterConsumptionIsNormal() throws Exception {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();

    MediaReading reading = new MediaReading();
    reading.setInitialReading(new BigDecimal("100.00"));
    reading.setInitialReadingStatus(ReadingStatus.AUTO_APPROVED);
    reading.setFinalReadingStatus(ReadingStatus.PENDING);
    reading.setUnitPrice(new BigDecimal("5.00"));

    Settlement settlement = new Settlement();
    settlement.setReservationId(reservationId);

    io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto reservation =
        new io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto();
    reservation.setStartDate(java.time.LocalDate.of(2026, 1, 1));
    reservation.setEndDate(java.time.LocalDate.of(2026, 1, 8)); // 7 days

    io.github.kwatera_project.kwatera.billing_service.dto.UnitDto unit =
        new io.github.kwatera_project.kwatera.billing_service.dto.UnitDto();
    unit.setCapacity(2); // 2 persons
    // expected = 2 * 7 * 0.1 = 1.4 m3, min = 0.28, max = 4.2
    // consumption = 101.0 - 100.0 = 1.0 m3 → NORMAL

    when(mediaReadingRepository.findBySettlementIdAndUtilityType(settlementId, UtilityType.WATER))
        .thenReturn(Optional.of(reading));
    when(ocrClient.readMeter(multipartFile))
        .thenReturn(new OcrResponseDto("101.00", new BigDecimal("0.99")));
    when(multipartFile.getBytes()).thenReturn(new byte[0]);
    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
    when(reservationClient.getReservation(reservationId)).thenReturn(reservation);
    when(propertyClient.getUnit(unitId)).thenReturn(unit);

    ReadingStatus status =
        mediaReadingService.processFinalReadingUpload(
            settlementId, unitId, UtilityType.WATER, multipartFile);

    assertEquals(ReadingStatus.AUTO_APPROVED, status);
    verify(settlementService)
        .addUtilitySettlementItem(any(), any(), eq(SettlementItemType.WATER), any(), any(), any());
  }
}
