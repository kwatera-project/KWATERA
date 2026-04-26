package io.github.kwatera_project.kwatera.reservation_service.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateReservationRequest {
  private UUID unitId;
  private LocalDate startDate;
  private LocalDate endDate;
}
