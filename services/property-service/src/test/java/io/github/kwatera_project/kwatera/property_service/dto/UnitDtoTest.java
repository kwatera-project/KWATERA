package io.github.kwatera_project.kwatera.property_service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UnitDtoTest {

  @Test
  void unitDto_shouldReturnValues() {
    UUID id = UUID.randomUUID();

    UnitDto dto = new UnitDto(id, "Room", "Nice room", BigDecimal.valueOf(100), 2, "img.jpg");

    assertEquals(id, dto.getId());
    assertEquals("Room", dto.getName());
    assertEquals("Nice room", dto.getDescription());
    assertEquals(BigDecimal.valueOf(100), dto.getPricePerNight());
    assertEquals(2, dto.getCapacity());
    assertEquals("img.jpg", dto.getImageUrl());
  }
}
