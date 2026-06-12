package io.github.kwatera_project.kwatera.property_service.dto;

import io.github.kwatera_project.kwatera.property_service.model.PropertyType;
import java.util.List;

public record PropertyCreateRequest(
    String title,
    String description,
    String city,
    String country,
    String postalCode,
    String street,
    String streetNumber,
    List<String> amenities,
    PropertyType propertyType) {

  public PropertyCreateRequest {
    amenities = amenities != null ? List.copyOf(amenities) : null;
  }
}
