package io.github.kwatera_project.kwatera.reservation_service.dto;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class ReservationDetailsDto {
  private UUID id;
  private UUID userId;
  private String guestEmail;
  private UUID unitId;
  private LocalDate startDate;
  private LocalDate endDate;
  private ReservationStatus status;
  private Instant createdAt;
  private BigDecimal pricePerNightSnapshot;
  private BigDecimal totalPrice;
  private BigDecimal convertedTotalPrice;
  private CurrencyMetadataDto currencyInfo;
  private String guestName;
  private String unitName;
  private String city;
  private String ownerName;
  private String ownerEmail;
}
