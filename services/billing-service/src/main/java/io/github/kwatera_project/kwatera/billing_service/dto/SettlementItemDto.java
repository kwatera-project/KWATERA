package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.math.BigDecimal;
import java.util.UUID;

public record SettlementItemDto(
    UUID id,
    UUID settlementId,
    SettlementItemType type,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal amount,
    BigDecimal convertedAmount,
    CurrencyMetadataDto currencyInfo) {
  public static SettlementItemDto from(
      SettlementItem item, BigDecimal convertedAmount, CurrencyMetadataDto currencyInfo) {
    return new SettlementItemDto(
        item.getId(),
        item.getSettlementId(),
        item.getType(),
        item.getDescription(),
        item.getQuantity(),
        item.getUnitPrice(),
        item.getAmount(),
        convertedAmount,
        currencyInfo);
  }
}
