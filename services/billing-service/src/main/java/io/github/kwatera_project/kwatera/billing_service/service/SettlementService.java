package io.github.kwatera_project.kwatera.billing_service.service;

import static io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType.*;
import static io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.*;

import io.github.kwatera_project.kwatera.billing_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.billing_service.dto.*;
import io.github.kwatera_project.kwatera.billing_service.event.SettlementEventPublisher;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementItemRepository;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SettlementService {

  private final SettlementRepository settlementRepository;
  private final SettlementItemRepository settlementItemRepository;
  private final SettlementEventPublisher settlementEventPublisher;

  private final PropertyClient propertyClient;

  private static final String SETTLEMENT_NOT_FOUND = "Settlement not found";

  @Transactional
  public void registerPayment(
      UUID settlementId,
      UUID unitId,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice) {
    Settlement settlement =
        settlementRepository
            .findById(settlementId)
            .orElseThrow(() -> new RuntimeException(SETTLEMENT_NOT_FOUND));

    SettlementItem item =
        createSettlementItem(settlementId, type, description, quantity, unitPrice);

    applyItemToSettlement(settlement, item);

    BigDecimal newPaid = settlement.getAmountPaid().add(item.getAmount());

    settlement.setAmountPaid(newPaid);

    recalculateTotals(settlement);
    recalculateSettlementStatus(settlement, unitId);

    if (newPaid.compareTo(settlement.getTotalAmount()) > 0) {
      throw new IllegalStateException("Payment exceeds settlement total");
    }

    settlementRepository.save(settlement);
  }

  @Transactional
  public SettlementItem addUtilityCharge(
      UUID settlementId,
      UUID unitId,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice) {
    if (type != WATER && type != ELECTRICITY) {
      throw new IllegalArgumentException(
          "Only water and electricity utility charges are supported");
    }

    Settlement settlement =
        settlementRepository
            .findById(settlementId)
            .orElseThrow(() -> new RuntimeException(SETTLEMENT_NOT_FOUND));

    SettlementItem item =
        createSettlementItem(settlementId, type, description, quantity, unitPrice);

    applyItemToSettlement(settlement, item);
    recalculateTotals(settlement);
    recalculateSettlementStatus(settlement, unitId);

    settlementRepository.save(settlement);

    return item;
  }

  private void applyItemToSettlement(Settlement settlement, SettlementItem item) {

    BigDecimal amount = item.getAmount();

    switch (item.getType()) {
      case DEPOSIT -> settlement.setDepositAmount(settlement.getDepositAmount().add(amount));

      case ELECTRICITY, WATER, CLEANING_FEE ->
          settlement.setUtilitiesAmount(settlement.getUtilitiesAmount().add(amount));

      case ACCOMMODATION -> {
        // usually already known at settlement creation
      }
    }
  }

  private SettlementStatus calculateStatus(Settlement settlement, boolean hasRequiredItems) {

    BigDecimal balance = settlement.getTotalAmount().subtract(settlement.getAmountPaid());

    if (balance.compareTo(BigDecimal.ZERO) <= 0 && hasRequiredItems) {
      return PAID;
    }

    boolean hasAdditionalCharges =
        settlementItemRepository.existsBySettlementIdAndTypeIn(
            settlement.getId(), List.of(ELECTRICITY, WATER, CLEANING_FEE));

    return hasAdditionalCharges ? ISSUED : PARTIALLY_PAID;
  }

  private void recalculateSettlementStatus(Settlement settlement, UUID unitId) {

    BigDecimal balance = settlement.getTotalAmount().subtract(settlement.getAmountPaid());

    settlement.setBalanceDue(balance.max(BigDecimal.ZERO));

    boolean hasRequiredItems;

    if (balance.compareTo(BigDecimal.ZERO) <= 0) {
      hasRequiredItems = hasAllRequiredItems(settlement, unitId);
    } else {
      hasRequiredItems = false;
    }

    SettlementStatus previous = settlement.getStatus();

    SettlementStatus newStatus = calculateStatus(settlement, hasRequiredItems);

    settlement.setStatus(newStatus);

    if (previous != newStatus) {
      settlementEventPublisher.publishSettlementStatusChanged(
          new SettlementStatusChangedEvent(settlement.getReservationId(), newStatus));
    }
  }

  private SettlementItem createSettlementItem(
      UUID settlementId,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice) {

    SettlementItem item = new SettlementItem();

    item.setSettlementId(settlementId);
    item.setType(type);
    item.setDescription(description);
    item.setQuantity(quantity);
    item.setUnitPrice(unitPrice);
    BigDecimal amount = unitPrice.multiply(quantity);
    item.setAmount(amount);

    return settlementItemRepository.save(item);
  }

  @Transactional
  public Settlement createSettlement(UUID reservationId, BigDecimal accommodationAmount) {
    return createSettlement(reservationId, accommodationAmount, BigDecimal.ZERO);
  }

  @Transactional
  public Settlement createSettlement(
      UUID reservationId, BigDecimal accommodationAmount, BigDecimal depositAmount) {
    Settlement settlement = new Settlement();

    settlement.setReservationId(reservationId);

    settlement.setAccommodationAmount(accommodationAmount);
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(depositAmount);
    settlement.setDiscountAmount(BigDecimal.ZERO);

    settlement.setAmountPaid(BigDecimal.ZERO);

    settlement.setStatus(DRAFT);

    recalculateTotals(settlement);

    return settlementRepository.save(settlement);
  }

  private void recalculateTotals(Settlement settlement) {
    BigDecimal total =
        settlement
            .getAccommodationAmount()
            .add(settlement.getUtilitiesAmount())
            .add(settlement.getDepositAmount())
            .subtract(settlement.getDiscountAmount());

    if (total.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalStateException("Settlement total cannot be negative");
    }

    settlement.setTotalAmount(total);

    BigDecimal balance = total.subtract(settlement.getAmountPaid());
    settlement.setBalanceDue(balance.max(BigDecimal.ZERO));
  }

  @Transactional
  public void applyDiscount(UUID settlementId, UUID unitId, BigDecimal discountAmount) {
    Settlement settlement =
        settlementRepository
            .findById(settlementId)
            .orElseThrow(() -> new RuntimeException(SETTLEMENT_NOT_FOUND));

    settlement.setDiscountAmount(discountAmount);

    recalculateTotals(settlement);
    recalculateSettlementStatus(settlement, unitId);

    settlementRepository.save(settlement);
  }

  public SettlementResponseDto getSettlementWithItems(UUID reservationId) {

    Settlement settlement =
        settlementRepository
            .findByReservationId(reservationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, SETTLEMENT_NOT_FOUND));

    List<SettlementItem> items = settlementItemRepository.findBySettlementId(settlement.getId());

    return new SettlementResponseDto(SettlementDto.from(settlement), items);
  }

  public SettlementItemDto getSettlementItemInfoByType(
      UUID reservationId, SettlementItemType settlementItemType) {

    Settlement settlement =
        settlementRepository
            .findByReservationId(reservationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, SETTLEMENT_NOT_FOUND));

    SettlementItem item =
        settlementItemRepository
            .findBySettlementIdAndType(settlement.getId(), settlementItemType)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement item not found"));

    return SettlementItemDto.from(item);
  }

  private boolean hasAllRequiredItems(Settlement settlement, UUID unitId) {

    Set<SettlementItemType> requiredTypes =
        propertyClient.getUnitSettlementItems(unitId).stream()
            .map(UnitSettlementItemDto::settlementItemType)
            .collect(Collectors.toSet());

    Set<SettlementItemType> existingTypes =
        settlementItemRepository.findBySettlementId(settlement.getId()).stream()
            .map(SettlementItem::getType)
            .collect(Collectors.toSet());

    return existingTypes.containsAll(requiredTypes);
  }
}
