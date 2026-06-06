package io.github.kwatera_project.kwatera.property_service.service;

import io.github.kwatera_project.kwatera.property_service.dto.Coordinates;

public interface GeocodingService {

  Coordinates getCoordinates(
      String street, String streetNumber, String postalCode, String city, String country);
}
