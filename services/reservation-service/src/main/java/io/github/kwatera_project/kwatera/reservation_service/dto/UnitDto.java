package io.github.kwatera_project.kwatera.reservation_service.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnitDto {
  private UUID id;
  private String name;
  private String description;
  private BigDecimal pricePerNight;
  private Integer capacity;
  private String imageUrl;
}
