package io.github.kwatera_project.kwatera.reservation_service.dto;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class ReservationDetailsDto {
  private UUID id;
  private UUID userId;
  private UUID unitId;
  private LocalDate startDate;
  private LocalDate endDate;
  private ReservationStatus status;
  private Instant createdAt;
}
