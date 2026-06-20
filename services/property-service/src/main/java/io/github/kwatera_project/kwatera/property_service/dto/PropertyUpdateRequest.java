package io.github.kwatera_project.kwatera.property_service.dto;

import java.util.List;
import java.util.Optional;

public record PropertyUpdateRequest(
    Optional<String> title,
    Optional<String> description,
    Optional<String> city,
    Optional<String> country,
    Optional<String> postalCode,
    Optional<String> street,
    Optional<String> streetNumber,
    Optional<List<String>> amenities) {}
