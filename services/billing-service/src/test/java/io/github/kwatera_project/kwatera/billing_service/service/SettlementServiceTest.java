package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementItemDto;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementResponseDto;
import io.github.kwatera_project.kwatera.billing_service.dto.UnitSettlementItemDto;
import io.github.kwatera_project.kwatera.billing_service.event.SettlementEventPublisher;
import io.github.kwatera_project.kwatera.billing_service.model.*;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementItemRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

  @Mock private SettlementRepository settlementRepository;

  @Mock private SettlementItemRepository settlementItemRepository;

  @Mock private SettlementEventPublisher settlementEventPublisher;

  @Mock private PropertyClient propertyClient;

  @InjectMocks private SettlementService settlementService;

  private Settlement baseSettlement(UUID id) {
    Settlement settlement = new Settlement();
    settlement.setId(id);
    settlement.setAccommodationAmount(BigDecimal.valueOf(500));
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(BigDecimal.ZERO);
    settlement.setDiscountAmount(BigDecimal.ZERO);
    settlement.setAmountPaid(BigDecimal.ZERO);
    settlement.setTotalAmount(BigDecimal.valueOf(500));
    settlement.setBalanceDue(BigDecimal.valueOf(500));
    return settlement;
  }

  @Test
  void shouldCreateSettlement() {
    UUID reservationId = UUID.randomUUID();

    Settlement saved = new Settlement();
    saved.setReservationId(reservationId);

    when(settlementRepository.save(any(Settlement.class))).thenReturn(saved);

    Settlement result =
        settlementService.createSettlement(
            reservationId, BigDecimal.valueOf(500), BigDecimal.valueOf(100));

    assertNotNull(result);
    assertEquals(reservationId, result.getReservationId());

    verify(settlementRepository).save(any(Settlement.class));
  }

  @Test
  void shouldRegisterPayment() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setAmountPaid(BigDecimal.ZERO);
    settlement.setAccommodationAmount(BigDecimal.valueOf(500));
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(BigDecimal.ZERO);
    settlement.setDiscountAmount(BigDecimal.ZERO);
    settlement.setTotalAmount(BigDecimal.valueOf(500));

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.save(any(SettlementItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(false);

    settlementService.registerPayment(
        settlementId,
        unitId,
        SettlementItemType.ACCOMMODATION,
        "Accommodation fee",
        BigDecimal.ONE,
        BigDecimal.valueOf(200));

    assertEquals(BigDecimal.valueOf(200), settlement.getAmountPaid());
    verify(settlementRepository).save(settlement);
  }

  @Test
  void shouldThrowWhenPaymentExceedsTotal() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setAmountPaid(BigDecimal.valueOf(450));
    settlement.setAccommodationAmount(BigDecimal.valueOf(500));
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(BigDecimal.ZERO);
    settlement.setDiscountAmount(BigDecimal.ZERO);
    settlement.setTotalAmount(BigDecimal.valueOf(500));

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    BigDecimal amount = BigDecimal.valueOf(100); // 450 + 100 = 550; 550 > 500

    assertThrows(
        IllegalStateException.class,
        () ->
            settlementService.registerPayment(
                settlementId,
                unitId,
                SettlementItemType.ACCOMMODATION,
                "fee",
                BigDecimal.ONE,
                amount));
  }

  @Test
  void shouldApplyDiscount() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setAccommodationAmount(BigDecimal.valueOf(500));
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(BigDecimal.ZERO);
    settlement.setDiscountAmount(BigDecimal.ZERO);
    settlement.setAmountPaid(BigDecimal.ZERO);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(false);

    settlementService.applyDiscount(settlementId, unitId, BigDecimal.valueOf(50));

    assertEquals(BigDecimal.valueOf(50), settlement.getDiscountAmount());
    assertEquals(BigDecimal.valueOf(450), settlement.getTotalAmount()); // 500 - 50 = 450
  }

  @Test
  void shouldReturnSettlementWithItems() {
    UUID reservationId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setReservationId(reservationId);

    SettlementItem item = new SettlementItem();
    item.setSettlementId(settlementId);

    when(settlementRepository.findByReservationId(reservationId))
        .thenReturn(Optional.of(settlement));

    when(settlementItemRepository.findBySettlementId(settlementId)).thenReturn(List.of(item));

    SettlementResponseDto result = settlementService.getSettlementWithItems(reservationId);

    assertNotNull(result);
  }

  @Test
  void shouldAddDepositAmountWhenPaymentTypeIsDeposit() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = baseSettlement(settlementId);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(false);

    settlementService.registerPayment(
        settlementId,
        unitId,
        SettlementItemType.DEPOSIT,
        "Deposit",
        BigDecimal.ONE,
        BigDecimal.valueOf(100));

    assertEquals(BigDecimal.valueOf(100), settlement.getDepositAmount());
  }

  @Test
  void shouldAddUtilitiesAmountWhenPaymentTypeIsWater() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = baseSettlement(settlementId);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(true);

    settlementService.registerPayment(
        settlementId,
        unitId,
        SettlementItemType.WATER,
        "Water",
        BigDecimal.ONE,
        BigDecimal.valueOf(50));

    assertEquals(BigDecimal.valueOf(50), settlement.getUtilitiesAmount());
  }

  @Test
  void shouldAddUtilityChargeWithoutIncreasingAmountPaid() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = baseSettlement(settlementId);
    settlement.setReservationId(UUID.randomUUID());
    settlement.setAmountPaid(BigDecimal.valueOf(500));
    settlement.setBalanceDue(BigDecimal.ZERO);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
    when(settlementItemRepository.save(any(SettlementItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(true);

    settlementService.addUtilityCharge(
        settlementId,
        unitId,
        SettlementItemType.WATER,
        "Water usage",
        BigDecimal.valueOf(5),
        BigDecimal.valueOf(20));

    assertEquals(BigDecimal.valueOf(100), settlement.getUtilitiesAmount());
    assertEquals(BigDecimal.valueOf(600), settlement.getTotalAmount());
    assertEquals(BigDecimal.valueOf(500), settlement.getAmountPaid());
    assertEquals(BigDecimal.valueOf(100), settlement.getBalanceDue());
    verify(settlementRepository).save(settlement);
  }

  @Test
  void shouldRecalculateBalanceDueWhenUtilityChargeAdded() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = baseSettlement(settlementId);
    settlement.setReservationId(UUID.randomUUID());
    settlement.setAmountPaid(BigDecimal.valueOf(200));
    settlement.setBalanceDue(BigDecimal.valueOf(300));

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
    when(settlementItemRepository.save(any(SettlementItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(true);

    settlementService.addUtilityCharge(
        settlementId,
        unitId,
        SettlementItemType.ELECTRICITY,
        "Electricity usage",
        BigDecimal.valueOf(10),
        BigDecimal.valueOf(10));

    assertEquals(BigDecimal.valueOf(100), settlement.getUtilitiesAmount());
    assertEquals(BigDecimal.valueOf(600), settlement.getTotalAmount());
    assertEquals(BigDecimal.valueOf(200), settlement.getAmountPaid());
    assertEquals(BigDecimal.valueOf(400), settlement.getBalanceDue());
    verify(settlementRepository).save(settlement);
  }

  @Test
  void shouldNotModifyAmountsForAccommodationPayment() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = baseSettlement(settlementId);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(false);

    settlementService.registerPayment(
        settlementId,
        unitId,
        SettlementItemType.ACCOMMODATION,
        "Accommodation",
        BigDecimal.ONE,
        BigDecimal.valueOf(100));

    assertEquals(BigDecimal.ZERO, settlement.getDepositAmount());
    assertEquals(BigDecimal.ZERO, settlement.getUtilitiesAmount());
  }

  @Test
  void shouldThrowWhenSettlementNotFound() {
    UUID reservationId = UUID.randomUUID();

    when(settlementRepository.findByReservationId(reservationId)).thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                settlementService.getSettlementItemInfoByType(
                    reservationId, SettlementItemType.ACCOMMODATION));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void shouldThrowWhenSettlementItemNotFound() {
    UUID reservationId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);

    when(settlementRepository.findByReservationId(reservationId))
        .thenReturn(Optional.of(settlement));

    when(settlementItemRepository.findBySettlementIdAndType(
            settlementId, SettlementItemType.ACCOMMODATION))
        .thenReturn(Optional.empty());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                settlementService.getSettlementItemInfoByType(
                    reservationId, SettlementItemType.ACCOMMODATION));

    assertEquals("Settlement item not found", ex.getReason());
  }

  private boolean invokeHasAllRequiredItems(
      SettlementService service, Settlement settlement, UUID unitId) {

    try {
      var method =
          SettlementService.class.getDeclaredMethod(
              "hasAllRequiredItems", Settlement.class, UUID.class);

      method.setAccessible(true);

      return (boolean) method.invoke(service, settlement, unitId);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void shouldReturnSettlementItemByType() {
    UUID reservationId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);

    SettlementItem item = new SettlementItem();
    item.setId(UUID.randomUUID());
    item.setSettlementId(settlementId);
    item.setType(SettlementItemType.ACCOMMODATION);

    when(settlementRepository.findByReservationId(reservationId))
        .thenReturn(Optional.of(settlement));

    when(settlementItemRepository.findBySettlementIdAndType(
            settlementId, SettlementItemType.ACCOMMODATION))
        .thenReturn(Optional.of(item));

    SettlementItemDto result =
        settlementService.getSettlementItemInfoByType(
            reservationId, SettlementItemType.ACCOMMODATION);

    assertNotNull(result);
  }

  @Test
  void shouldReturnTrueWhenAllRequiredItemsExist() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);

    when(propertyClient.getUnitSettlementItems(unitId))
        .thenReturn(
            List.of(
                new UnitSettlementItemDto(
                    UUID.randomUUID(),
                    unitId,
                    SettlementItemType.WATER,
                    BigDecimal.valueOf(20),
                    MeasurementUnit.M3,
                    BillingType.PER_USAGE)));

    SettlementItem item = new SettlementItem();
    item.setType(SettlementItemType.WATER);

    when(settlementItemRepository.findBySettlementId(settlementId)).thenReturn(List.of(item));

    boolean result = invokeHasAllRequiredItems(settlementService, settlement, unitId);

    assertTrue(result);
  }

  @Test
  void shouldReturnFalseWhenMissingRequiredItems() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);

    when(propertyClient.getUnitSettlementItems(unitId))
        .thenReturn(
            List.of(
                new UnitSettlementItemDto(
                    UUID.randomUUID(),
                    unitId,
                    SettlementItemType.WATER,
                    BigDecimal.valueOf(20),
                    MeasurementUnit.M3,
                    BillingType.PER_USAGE),
                new UnitSettlementItemDto(
                    UUID.randomUUID(),
                    unitId,
                    SettlementItemType.DEPOSIT,
                    BigDecimal.valueOf(50),
                    null,
                    BillingType.FIXED)));

    SettlementItem item = new SettlementItem();
    item.setType(SettlementItemType.WATER);

    when(settlementItemRepository.findBySettlementId(settlementId)).thenReturn(List.of(item));

    boolean result = invokeHasAllRequiredItems(settlementService, settlement, unitId);

    assertFalse(result);
  }

  @Test
  void shouldRegisterWaterPaymentWhenAccommodationAlreadyExists() {
    UUID settlementId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);

    settlement.setAccommodationAmount(BigDecimal.valueOf(500));
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(BigDecimal.ZERO);
    settlement.setDiscountAmount(BigDecimal.ZERO);

    settlement.setAmountPaid(BigDecimal.valueOf(500));

    settlement.setTotalAmount(BigDecimal.valueOf(500));
    settlement.setBalanceDue(BigDecimal.ZERO);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.save(any(SettlementItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    when(propertyClient.getUnitSettlementItems(unitId))
        .thenReturn(
            List.of(
                new UnitSettlementItemDto(
                    UUID.randomUUID(),
                    unitId,
                    SettlementItemType.WATER,
                    BigDecimal.valueOf(20),
                    MeasurementUnit.M3,
                    BillingType.PER_USAGE)));

    SettlementItem waterItem = new SettlementItem();
    waterItem.setType(SettlementItemType.WATER);

    when(settlementItemRepository.findBySettlementId(settlementId)).thenReturn(List.of(waterItem));

    settlementService.registerPayment(
        settlementId,
        unitId,
        SettlementItemType.WATER,
        "Water usage",
        BigDecimal.valueOf(5),
        BigDecimal.valueOf(20));

    // utilities updated: 20 * 5
    assertEquals(BigDecimal.valueOf(100), settlement.getUtilitiesAmount());

    // total recalculated: 500 + 100
    assertEquals(BigDecimal.valueOf(600), settlement.getTotalAmount());

    // paid recalculated: 500 + 100
    assertEquals(BigDecimal.valueOf(600), settlement.getAmountPaid());

    // fully paid again
    assertEquals(BigDecimal.ZERO, settlement.getBalanceDue());

    // all required items exist + fully paid
    assertEquals(SettlementStatus.PAID, settlement.getStatus());

    verify(settlementRepository).save(settlement);
  }
}
