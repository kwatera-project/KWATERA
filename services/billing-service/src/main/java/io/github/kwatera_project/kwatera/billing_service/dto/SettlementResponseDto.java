package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import java.util.List;

public record SettlementResponseDto(SettlementDto settlement, List<SettlementItem> items) {
  public SettlementResponseDto {
    items = List.copyOf(items);
  }
}
