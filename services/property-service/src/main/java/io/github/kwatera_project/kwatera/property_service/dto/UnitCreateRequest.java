package io.github.kwatera_project.kwatera.property_service.dto;

import io.github.kwatera_project.kwatera.property_service.model.UnitType;
import java.math.BigDecimal;

public record UnitCreateRequest(
    String name,
    String description,
    BigDecimal pricePerNight,
    Integer capacity,
    UnitType unitType,
    String unitNumber,
    Integer floor) {}
