package io.github.kwatera_project.kwatera.billing_service.service;

import static io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.DRAFT;
import static io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.PAID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.dto.SettlementResponseDto;
import io.github.kwatera_project.kwatera.billing_service.event.SettlementEventPublisher;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
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

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

  @Mock private SettlementRepository settlementRepository;

  @Mock private SettlementItemRepository settlementItemRepository;

  @Mock private SettlementEventPublisher settlementEventPublisher;

  @InjectMocks private SettlementService settlementService;

  private Settlement baseSettlement(UUID id) {
    Settlement settlement = new Settlement();
    settlement.setId(id);
    settlement.setFinalized(false);
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

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setFinalized(false);
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

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setFinalized(false);
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
                settlementId, SettlementItemType.ACCOMMODATION, "fee", BigDecimal.ONE, amount));
  }

  @Test
  void shouldFinalizeSettlement() {
    UUID settlementId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setFinalized(false);
    settlement.setAmountPaid(BigDecimal.ZERO);
    settlement.setAccommodationAmount(BigDecimal.valueOf(500));
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(BigDecimal.ZERO);
    settlement.setDiscountAmount(BigDecimal.ZERO);
    settlement.setTotalAmount(BigDecimal.valueOf(500));

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(false);

    settlementService.finalizeSettlement(settlementId);

    assertTrue(settlement.getFinalized());
    assertNotNull(settlement.getIssuedAt());

    verify(settlementRepository).save(settlement);
  }

  @Test
  void shouldApplyDiscount() {
    UUID settlementId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setFinalized(false);
    settlement.setAccommodationAmount(BigDecimal.valueOf(500));
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(BigDecimal.ZERO);
    settlement.setDiscountAmount(BigDecimal.ZERO);
    settlement.setAmountPaid(BigDecimal.ZERO);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(false);

    settlementService.applyDiscount(settlementId, BigDecimal.valueOf(50));

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

    Settlement settlement = baseSettlement(settlementId);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(false);

    settlementService.registerPayment(
        settlementId,
        SettlementItemType.DEPOSIT,
        "Deposit",
        BigDecimal.ONE,
        BigDecimal.valueOf(100));

    assertEquals(BigDecimal.valueOf(100), settlement.getDepositAmount());
  }

  @Test
  void shouldAddUtilitiesAmountWhenPaymentTypeIsWater() {
    UUID settlementId = UUID.randomUUID();

    Settlement settlement = baseSettlement(settlementId);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(true);

    settlementService.registerPayment(
        settlementId, SettlementItemType.WATER, "Water", BigDecimal.ONE, BigDecimal.valueOf(50));

    assertEquals(BigDecimal.valueOf(50), settlement.getUtilitiesAmount());
  }

  @Test
  void shouldNotModifyAmountsForAccommodationPayment() {
    UUID settlementId = UUID.randomUUID();

    Settlement settlement = baseSettlement(settlementId);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(settlementItemRepository.existsBySettlementIdAndTypeIn(any(), any())).thenReturn(false);

    settlementService.registerPayment(
        settlementId,
        SettlementItemType.ACCOMMODATION,
        "Accommodation",
        BigDecimal.ONE,
        BigDecimal.valueOf(100));

    assertEquals(BigDecimal.ZERO, settlement.getDepositAmount());
    assertEquals(BigDecimal.ZERO, settlement.getUtilitiesAmount());
  }

  @Test
  void shouldMarkSettlementAsPaidWhenBalanceZeroAndFinalized() {
    UUID settlementId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setTotalAmount(BigDecimal.valueOf(100));
    settlement.setAmountPaid(BigDecimal.valueOf(100));
    settlement.setFinalized(false);
    settlement.setStatus(DRAFT);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));

    when(settlementRepository.save(any(Settlement.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    settlementService.finalizeSettlement(settlementId);

    assertEquals(PAID, settlement.getStatus());
    assertNotNull(settlement.getPaidAt());
    assertTrue(settlement.getFinalized());
  }
}
