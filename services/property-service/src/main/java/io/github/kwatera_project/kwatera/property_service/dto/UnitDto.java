package io.github.kwatera_project.kwatera.property_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class UnitDto {

  private UUID id;
  private String name;
  private String description;
  private BigDecimal pricePerNight;
  private Integer capacity;

  public UnitDto(
      UUID id, String name, String description, BigDecimal pricePerNight, Integer capacity) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.pricePerNight = pricePerNight;
    this.capacity = capacity;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getPricePerNight() {
    return pricePerNight;
  }

  public Integer getCapacity() {
    return capacity;
  }
}
