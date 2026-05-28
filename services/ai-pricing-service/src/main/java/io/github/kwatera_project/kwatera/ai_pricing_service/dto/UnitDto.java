package io.github.kwatera_project.kwatera.ai_pricing_service.dto;

import io.github.kwatera_project.kwatera.ai_pricing_service.model.UnitType;
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
  private UnitType unitType;

  public UnitDto(
      UUID id,
      String name,
      String description,
      BigDecimal pricePerNight,
      Integer capacity,
      String imageUrl,
      UnitType unitType) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.pricePerNight = pricePerNight;
    this.capacity = capacity;
    this.imageUrl = imageUrl;
    this.unitType = unitType;
  }
}
