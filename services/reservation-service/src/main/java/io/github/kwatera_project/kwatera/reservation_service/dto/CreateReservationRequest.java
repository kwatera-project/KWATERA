package io.github.kwatera_project.kwatera.reservation_service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateReservationRequest {

  @NotNull(message = "Unit id is required")
  private UUID unitId;

  @NotNull(message = "Start date is required")
  private LocalDate startDate;

  @NotNull(message = "End date is required")
  private LocalDate endDate;

  private String currency;
}
