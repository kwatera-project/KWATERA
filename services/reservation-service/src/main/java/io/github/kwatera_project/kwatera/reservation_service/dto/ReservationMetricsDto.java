package io.github.kwatera_project.kwatera.reservation_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationMetricsDto {
  private Long totalReservations;
  private Double occupancyRate;
  private Long occupiedDays;
}
