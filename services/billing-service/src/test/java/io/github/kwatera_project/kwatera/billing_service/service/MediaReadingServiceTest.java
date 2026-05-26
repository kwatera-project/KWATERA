package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.billing_service.model.*;
import io.github.kwatera_project.kwatera.billing_service.repository.MediaReadingRepository;
import java.math.BigDecimal;
import java.util.Optional;
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
    BigDecimal initialValue = new BigDecimal("100.000000");
    BigDecimal finalValue = new BigDecimal("108.000000");
    BigDecimal diff = new BigDecimal("8.000000");
    BigDecimal unitPrice = new BigDecimal("5.00");

    MediaReading existingReading = new MediaReading();
    existingReading.setSettlementId(settlementId);
    existingReading.setUtilityType(UtilityType.WATER);
    existingReading.setInitialReading(initialValue);
    existingReading.setUnitPrice(unitPrice);

    // Ustawiamy ręcznie wynik różnicy, ponieważ Mockito nie uruchomi logiki DB/Entity
    existingReading.setConsumptionDifference(diff);

    UUID settlementItemId = UUID.randomUUID();
    SettlementItem mockItem = new SettlementItem();
    mockItem.setId(settlementItemId);

    when(mediaReadingRepository.findBySettlementId(settlementId))
        .thenReturn(Optional.of(existingReading));

    when(settlementService.addUtilitySettlementItem(
            eq(settlementId),
            eq(unitId),
            eq(SettlementItemType.WATER),
            eq("Water usage"),
            eq(diff),
            eq(unitPrice)))
        .thenReturn(mockItem);

    when(mediaReadingRepository.save(any(MediaReading.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mediaReadingService.addFinalMediaReading(
        settlementId, unitId, finalValue, new BigDecimal("0.99"));

    verify(settlementService)
        .addUtilitySettlementItem(
            eq(settlementId),
            eq(unitId),
            eq(SettlementItemType.WATER),
            anyString(),
            eq(diff),
            eq(unitPrice));

    assertEquals(finalValue, existingReading.getFinalReading());
    assertEquals(settlementId, existingReading.getSettlementId());
  }

  @Test
  void shouldCreateAndSaveInitialMediaReading() {

    UUID settlementId = UUID.randomUUID();
    UtilityType utilityType = UtilityType.WATER;
    BigDecimal initialReading = new BigDecimal("100.00");
    BigDecimal confidenceScore = new BigDecimal("0.98");
    BigDecimal unitPrice = new BigDecimal("5.50");
    ReadingSource source = ReadingSource.OCR;

    ArgumentCaptor<MediaReading> readingCaptor = ArgumentCaptor.forClass(MediaReading.class);

    when(mediaReadingRepository.save(any(MediaReading.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mediaReadingService.createMediaReadingWithInitialReading(
        settlementId, utilityType, initialReading, confidenceScore, unitPrice, source);

    verify(mediaReadingRepository, times(1)).save(readingCaptor.capture());

    MediaReading savedReading = readingCaptor.getValue();

    assertEquals(settlementId, savedReading.getSettlementId());
    assertEquals(utilityType, savedReading.getUtilityType());
    assertEquals(initialReading, savedReading.getInitialReading());
    assertEquals(confidenceScore, savedReading.getInitialConfidenceScore());
    assertEquals(unitPrice, savedReading.getUnitPrice());
    assertEquals(source, savedReading.getReadingSource());

    assertEquals(ReadingStatus.PENDING, savedReading.getReadingStatus());
  }

  @Test
  void shouldRejectFinalReadingLowerThanInitial() {

    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    BigDecimal initialReading = new BigDecimal("100.00");
    BigDecimal invalidLowerFinalReading = new BigDecimal("90.00"); // Wartość mniejsza niż initial

    MediaReading existingReading = new MediaReading();
    existingReading.setSettlementId(settlementId);
    existingReading.setInitialReading(initialReading);

    when(mediaReadingRepository.findBySettlementId(settlementId))
        .thenReturn(Optional.of(existingReading));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              mediaReadingService.addFinalMediaReading(
                  settlementId, unitId, invalidLowerFinalReading, new BigDecimal("0.99"));
            });

    assertEquals("Final reading cannot be lower than initial reading", exception.getMessage());

    verify(mediaReadingRepository, never()).save(any());
    verify(settlementService, never())
        .addUtilitySettlementItem(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldMapElectricityReadingToElectricitySettlementItem() {

    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    BigDecimal initialReading = new BigDecimal("1000.00");
    BigDecimal finalReading = new BigDecimal("1250.00");
    BigDecimal consumptionDiff = new BigDecimal("250.00");
    BigDecimal unitPrice = new BigDecimal("0.90");

    MediaReading electricityReading = new MediaReading();
    electricityReading.setSettlementId(settlementId);
    electricityReading.setUtilityType(UtilityType.ELECTRICITY);
    electricityReading.setInitialReading(initialReading);
    electricityReading.setUnitPrice(unitPrice);
    electricityReading.setConsumptionDifference(consumptionDiff);

    when(mediaReadingRepository.findBySettlementId(settlementId))
        .thenReturn(Optional.of(electricityReading));

    mediaReadingService.addFinalMediaReading(
        settlementId, unitId, finalReading, new BigDecimal("0.95"));

    verify(settlementService)
        .addUtilitySettlementItem(
            eq(settlementId),
            eq(unitId),
            eq(SettlementItemType.ELECTRICITY),
            eq("Electricity usage"),
            eq(consumptionDiff),
            eq(unitPrice));

    assertEquals(finalReading, electricityReading.getFinalReading());
  }
}
