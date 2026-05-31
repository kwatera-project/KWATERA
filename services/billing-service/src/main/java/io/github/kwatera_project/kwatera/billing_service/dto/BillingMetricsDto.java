package io.github.kwatera_project.kwatera.billing_service.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingMetricsDto {
  private BigDecimal revenueFromSettlements;
  private BigDecimal unpaidBalance;
  private Long paidSettlementsCount;
  private Long unpaidSettlementsCount;
}
