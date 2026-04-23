package io.github.kwatera_project.kwatera.property_service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PropertyDtoTest {

  @Test
  void propertyDto_shouldReturnValues() {
    UUID id = UUID.randomUUID();

    PropertyDto dto = new PropertyDto(id, "Title", "Desc", "Location", "img.jpg");

    assertEquals(id, dto.getId());
    assertEquals("Title", dto.getTitle());
    assertEquals("Desc", dto.getDescription());
    assertEquals("Location", dto.getLocation());
    assertEquals("img.jpg", dto.getImageUrl());
  }
}
