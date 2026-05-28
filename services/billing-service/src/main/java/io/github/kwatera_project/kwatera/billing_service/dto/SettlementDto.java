package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementDto(
    UUID id,
    UUID reservationId,
    SettlementStatus status,
    BigDecimal accommodationAmount,
    BigDecimal utilitiesAmount,
    BigDecimal depositAmount,
    BigDecimal discountAmount,
    BigDecimal totalAmount,
    BigDecimal amountPaid,
    BigDecimal balanceDue,
    BigDecimal convertedTotalAmount,
    BigDecimal convertedAmountPaid,
    BigDecimal convertedBalanceDue,
    BigDecimal convertedAccommodationAmount,
    BigDecimal convertedUtilitiesAmount,
    BigDecimal convertedDepositAmount,
    CurrencyMetadataDto currencyInfo,
    Instant issuedAt,
    Instant paidAt,
    Instant createdAt,
    Instant updatedAt) {

  public static SettlementDto from(
      Settlement s,
      BigDecimal convertedTotalAmount,
      BigDecimal convertedAmountPaid,
      BigDecimal convertedBalanceDue,
      BigDecimal convertedAccommodationAmount,
      BigDecimal convertedUtilitiesAmount,
      BigDecimal convertedDepositAmount,
      CurrencyMetadataDto currencyInfo) {
    return new SettlementDto(
        s.getId(),
        s.getReservationId(),
        s.getStatus(),
        s.getAccommodationAmount(),
        s.getUtilitiesAmount(),
        s.getDepositAmount(),
        s.getDiscountAmount(),
        s.getTotalAmount(),
        s.getAmountPaid(),
        s.getBalanceDue(),
        convertedTotalAmount,
        convertedAmountPaid,
        convertedBalanceDue,
        convertedAccommodationAmount,
        convertedUtilitiesAmount,
        convertedDepositAmount,
        currencyInfo,
        s.getIssuedAt(),
        s.getPaidAt(),
        s.getCreatedAt(),
        s.getUpdatedAt());
  }
}
