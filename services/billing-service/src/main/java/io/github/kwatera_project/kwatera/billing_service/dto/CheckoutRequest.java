package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
  private SettlementItemType type;
  private String description;
  private BigDecimal quantity;
  private BigDecimal unitPrice;
}
