package io.github.kwatera_project.kwatera.property_service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kwatera_project.kwatera.property_service.model.UnitType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UnitDtoTest {

  @Test
  void unitDto_shouldReturnValues() {
    UUID id = UUID.randomUUID();

    UnitDto dto =
        new UnitDto(
            id,
            "Room",
            "Nice room",
            BigDecimal.valueOf(100),
            2,
            "img.jpg",
            UUID.randomUUID(),
            UnitType.ENTIRE_RENTAL_UNIT,
            "10A",
            4,
            BigDecimal.valueOf(200),
            new CurrencyMetadataDto("PLN", "PLN", BigDecimal.ONE, java.time.LocalDate.now()),
            List.of("WiFi"),
            null,
            null);

    assertEquals(id, dto.getId());
    assertEquals("Room", dto.getName());
    assertEquals("Nice room", dto.getDescription());
    assertEquals(BigDecimal.valueOf(100), dto.getPricePerNight());
    assertEquals(2, dto.getCapacity());
    assertEquals("img.jpg", dto.getImageUrl());
    assertEquals(UnitType.ENTIRE_RENTAL_UNIT, dto.getUnitType());
    assertEquals("10A", dto.getUnitNumber());
    assertEquals(4, dto.getFloor());
    assertEquals(BigDecimal.valueOf(200), dto.getConvertedPricePerNight());
    assertEquals(
        new CurrencyMetadataDto("PLN", "PLN", BigDecimal.ONE, java.time.LocalDate.now()),
        dto.getCurrencyInfo());
    assertEquals(List.of("WiFi"), dto.getAmenities());
  }
}
