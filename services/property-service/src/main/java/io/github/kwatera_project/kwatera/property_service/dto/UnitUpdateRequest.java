package io.github.kwatera_project.kwatera.property_service.dto;

import io.github.kwatera_project.kwatera.property_service.model.UnitType;
import java.math.BigDecimal;
import java.util.Optional;

public record UnitUpdateRequest(
    Optional<String> name,
    Optional<String> description,
    Optional<BigDecimal> pricePerNight,
    Optional<Integer> capacity,
    Optional<UnitType> unitType,
    Optional<String> unitNumber,
    Optional<Integer> floor) {}
