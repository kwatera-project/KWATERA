package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.model.MediaReading;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingSource;
import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaReadingServiceTest {

  @Mock private MediaReadingRepository mediaReadingRepository;

  @Mock private SettlementService settlementService;

  @InjectMocks private MediaReadingService mediaReadingService;

  @Test
  void shouldCreateSettlementItemFromFinalizedWaterReading() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID settlementItemId = UUID.randomUUID();
    SettlementItem item = new SettlementItem();
    item.setId(settlementItemId);

    when(settlementService.addUtilityCharge(
            eq(settlementId),
            eq(unitId),
            eq(SettlementItemType.WATER),
            eq("Water usage"),
            eq(new BigDecimal("8.000000")),
            eq(new BigDecimal("5.00"))))
        .thenReturn(item);
    when(mediaReadingRepository.save(any(MediaReading.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MediaReading reading =
        mediaReadingService.createFinalizedMediaReadingCharge(
            settlementId,
            unitId,
            UtilityType.WATER,
            new BigDecimal("100.000000"),
            new BigDecimal("0.982145"),
            new BigDecimal("108.000000"),
            new BigDecimal("0.997531"),
            new BigDecimal("5.00"),
            ReadingSource.OCR,
            ReadingStatus.AUTO_APPROVED);

    assertEquals(settlementItemId, reading.getSettlementItemId());
    assertEquals(UtilityType.WATER, reading.getUtilityType());
    verify(settlementService)
        .addUtilityCharge(
            settlementId,
            unitId,
            SettlementItemType.WATER,
            "Water usage",
            new BigDecimal("8.000000"),
            new BigDecimal("5.00"));
  }

  @Test
  void shouldSaveMediaReadingLinkedToCreatedSettlementItem() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID settlementItemId = UUID.randomUUID();
    SettlementItem item = new SettlementItem();
    item.setId(settlementItemId);
    ArgumentCaptor<MediaReading> readingCaptor = ArgumentCaptor.forClass(MediaReading.class);

    when(settlementService.addUtilityCharge(any(), any(), any(), any(), any(), any()))
        .thenReturn(item);
    when(mediaReadingRepository.save(any(MediaReading.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mediaReadingService.createFinalizedMediaReadingCharge(
        settlementId,
        unitId,
        UtilityType.WATER,
        new BigDecimal("100.000000"),
        new BigDecimal("0.982145"),
        new BigDecimal("108.000000"),
        new BigDecimal("0.997531"),
        new BigDecimal("5.00"),
        ReadingSource.OCR,
        ReadingStatus.AUTO_APPROVED);

    verify(mediaReadingRepository).save(readingCaptor.capture());
    MediaReading savedReading = readingCaptor.getValue();

    assertEquals(settlementItemId, savedReading.getSettlementItemId());
    assertEquals(new BigDecimal("100.000000"), savedReading.getInitialReading());
    assertEquals(new BigDecimal("108.000000"), savedReading.getFinalReading());
    assertEquals(new BigDecimal("5.00"), savedReading.getUnitPrice());
    assertEquals(ReadingStatus.AUTO_APPROVED, savedReading.getReadingStatus());
  }

  @Test
  void shouldRejectFinalReadingLowerThanInitial() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            mediaReadingService.createFinalizedMediaReadingCharge(
                settlementId,
                unitId,
                UtilityType.WATER,
                new BigDecimal("108.000000"),
                new BigDecimal("0.982145"),
                new BigDecimal("100.000000"),
                new BigDecimal("0.997531"),
                new BigDecimal("5.00"),
                ReadingSource.OCR,
                ReadingStatus.AUTO_APPROVED));

    verify(settlementService, never()).addUtilityCharge(any(), any(), any(), any(), any(), any());
    verify(mediaReadingRepository, never()).save(any());
  }

  @Test
  void shouldMapElectricityReadingToElectricitySettlementItem() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    SettlementItem item = new SettlementItem();
    item.setId(UUID.randomUUID());

    when(settlementService.addUtilityCharge(
            eq(settlementId),
            eq(unitId),
            eq(SettlementItemType.ELECTRICITY),
            eq("Electricity usage"),
            eq(new BigDecimal("40.000000")),
            eq(new BigDecimal("1.20"))))
        .thenReturn(item);
    when(mediaReadingRepository.save(any(MediaReading.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mediaReadingService.createFinalizedMediaReadingCharge(
        settlementId,
        unitId,
        UtilityType.ELECTRICITY,
        new BigDecimal("500.000000"),
        new BigDecimal("0.982145"),
        new BigDecimal("540.000000"),
        new BigDecimal("0.997531"),
        new BigDecimal("1.20"),
        ReadingSource.OCR,
        ReadingStatus.AUTO_APPROVED);

    verify(settlementService)
        .addUtilityCharge(
            settlementId,
            unitId,
            SettlementItemType.ELECTRICITY,
            "Electricity usage",
            new BigDecimal("40.000000"),
            new BigDecimal("1.20"));
  }
}
