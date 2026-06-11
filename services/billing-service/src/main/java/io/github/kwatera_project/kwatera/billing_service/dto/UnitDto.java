package io.github.kwatera_project.kwatera.billing_service.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class UnitDto {

  private UUID id;

  private Integer capacity;
}
