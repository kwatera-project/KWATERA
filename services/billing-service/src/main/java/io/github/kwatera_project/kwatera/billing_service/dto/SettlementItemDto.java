package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.math.BigDecimal;
import java.util.UUID;

public record SettlementItemDto(
    UUID id,
    SettlementItemType type,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal amount) {
  public static SettlementItemDto from(SettlementItem s) {
    return new SettlementItemDto(
        s.getId(),
        s.getType(),
        s.getDescription(),
        s.getQuantity(),
        s.getUnitPrice(),
        s.getAmount());
  }
}
