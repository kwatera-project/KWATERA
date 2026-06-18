package io.github.kwatera_project.kwatera.property_service.dto;

import io.github.kwatera_project.kwatera.property_service.model.UnitType;
import java.math.BigDecimal;
import java.util.List;

public record UnitCreateRequest(
    String name,
    String description,
    BigDecimal pricePerNight,
    Integer capacity,
    UnitType unitType,
    String unitNumber,
    Integer floor,
    List<String> amenities,
    Integer bedrooms,
    Integer beds) {

  public UnitCreateRequest {
    amenities = amenities != null ? List.copyOf(amenities) : null;
  }
}
