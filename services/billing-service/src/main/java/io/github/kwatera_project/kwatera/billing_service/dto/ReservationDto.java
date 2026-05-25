package io.github.kwatera_project.kwatera.billing_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {
  private UUID id;
  private UUID userId;
  private UUID unitId;
  private LocalDate startDate;
  private LocalDate endDate;
  private String status;
  private Instant createdAt;
  private BigDecimal pricePerNightSnapshot;
  private BigDecimal totalPrice;
  private String paymentCurrency;
  private BigDecimal paymentExchangeRate;
}
