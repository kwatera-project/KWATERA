package io.github.kwatera_project.kwatera.property_service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PropertyDtoTest {

  @Test
  void propertyDto_shouldReturnValues() {
    UUID id = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();

    PropertyDto dto =
        new PropertyDto(
            id,
            ownerId,
            "Title",
            "Desc",
            "City",
            "img.jpg",
            BigDecimal.valueOf(200),
            BigDecimal.valueOf(400),
            "Poland",
            "89-678",
            "Street",
            "12F",
            List.of("WiFi"));

    assertEquals(id, dto.getId());
    assertEquals(ownerId, dto.getOwnerId());
    assertEquals("Title", dto.getTitle());
    assertEquals("Desc", dto.getDescription());
    assertEquals("City", dto.getCity());
    assertEquals("img.jpg", dto.getImageUrl());
    assertEquals(BigDecimal.valueOf(200), dto.getLatitude());
    assertEquals(BigDecimal.valueOf(400), dto.getLongitude());
    assertEquals("Poland", dto.getCountry());
    assertEquals("89-678", dto.getPostalCode());
    assertEquals("Street", dto.getStreet());
    assertEquals("12F", dto.getStreetNumber());
    assertEquals(List.of("WiFi"), dto.getAmenities());
  }
}
