package io.github.kwatera_project.kwatera.property_service.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitSettlementItemDto;
import io.github.kwatera_project.kwatera.property_service.model.PropertyImage;
import io.github.kwatera_project.kwatera.property_service.model.UnitType;
import io.github.kwatera_project.kwatera.property_service.service.PropertyService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PropertyControllerTest {

  private final PropertyService service = mock(PropertyService.class);
  private final PropertyController controller = new PropertyController(service);

  @Test
  void getAllProperties_shouldReturnList() {
    PropertyDto dto =
        new PropertyDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Test",
            "Desc",
            "Warsaw",
            "img",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "Poland",
            "",
            "",
            "");

    when(service.getAll()).thenReturn(List.of(dto));

    var result = controller.getAllProperties();

    assertEquals(1, result.size());
    assertEquals("Test", result.get(0).getTitle());
  }

  @Test
  void getPropertyById_shouldReturnProperty() {
    UUID id = UUID.randomUUID();

    PropertyDto dto =
        new PropertyDto(
            id,
            UUID.randomUUID(),
            "Test",
            "Desc",
            "Warsaw",
            "img",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "Poland",
            "",
            "",
            "");

    when(service.getById(id)).thenReturn(dto);

    var result = controller.getPropertyById(id);

    assertEquals(id, result.getId());
  }

  @Test
  void getUnits_shouldReturnUnits() {
    UUID propertyId = UUID.randomUUID();

    UnitDto unit =
        new UnitDto(
            UUID.randomUUID(),
            "Room",
            "Desc",
            BigDecimal.valueOf(200),
            2,
            "img.jpg",
            propertyId,
            UnitType.ENTIRE_RENTAL_UNIT,
            "10A",
            4,
            BigDecimal.valueOf(200),
            new io.github.kwatera_project.kwatera.property_service.dto.CurrencyMetadataDto(
                "PLN", "PLN", BigDecimal.ONE, java.time.LocalDate.now()));

    when(service.getUnits(propertyId, "PLN")).thenReturn(List.of(unit));

    var result = controller.getUnits(propertyId, "PLN");

    assertEquals(1, result.size());
    assertEquals("Room", result.get(0).getName());
  }

  @Test
  void getUnit_shouldReturnUnit() {
    UUID id = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();

    UnitDto unit =
        new UnitDto(
            id,
            "Room",
            "Desc",
            BigDecimal.valueOf(200),
            2,
            "img.jpg",
            propertyId,
            UnitType.ENTIRE_RENTAL_UNIT,
            "10A",
            4,
            BigDecimal.valueOf(200),
            new io.github.kwatera_project.kwatera.property_service.dto.CurrencyMetadataDto(
                "PLN", "PLN", BigDecimal.ONE, java.time.LocalDate.now()));

    when(service.getUnitById(id, "PLN")).thenReturn(unit);

    var result = controller.getUnit(id, "PLN");

    assertEquals(id, result.getId());
  }

  @Test
  void getUnitIdsByOwnerId_shouldReturnIds() {
    UUID ownerId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(service.getUnitIdsByOwnerId(ownerId)).thenReturn(List.of(unitId));

    var result = controller.getUnitIdsByOwnerId(ownerId);

    assertEquals(1, result.size());
    assertEquals(unitId, result.get(0));
  }

  @Test
  void getPropertyImages_shouldReturnImages() {
    UUID propertyId = UUID.randomUUID();

    PropertyImage mockImage = new PropertyImage();
    mockImage.setId(UUID.randomUUID());
    mockImage.setUrl(
        "http://localhost:8083/properties/aaaa1111-1111-1111-1111-111111111111/4dc49eff-91e1-45db-8aab-dd61bd758fb5.jpg");
    mockImage.setIsMain(true);
    mockImage.setPropertyId(propertyId);

    List<PropertyImage> mockImages = List.of(mockImage);

    when(service.getPropertyImages(propertyId)).thenReturn(mockImages);

    var result = controller.getPropertyImages(propertyId);

    assertEquals(1, result.size());
    assertEquals(
        "http://localhost:8083/properties/aaaa1111-1111-1111-1111-111111111111/4dc49eff-91e1-45db-8aab-dd61bd758fb5.jpg",
        result.get(0).getUrl());
  }

  @Test
  void getUnitSettlementItems_shouldReturnItems() {
    UUID unitId = UUID.randomUUID();

    UnitSettlementItemDto item = mock(UnitSettlementItemDto.class);

    when(service.getUnitSettlementItems(unitId)).thenReturn(List.of(item));

    var result = controller.getUnitSettlementItems(unitId);

    assertEquals(1, result.size());
    assertSame(item, result.get(0));

    verify(service, times(1)).getUnitSettlementItems(unitId);
  }

  @Test
  void getAllUnitIds_shouldReturnUnitIds() {
    UUID unitId = UUID.randomUUID();

    when(service.getAllUnitIds()).thenReturn(List.of(unitId));

    var result = controller.getAllUnitIds();

    assertEquals(1, result.size());
    assertEquals(unitId, result.get(0));
  }

  @Test
  void getUnitImages_shouldReturnMappedUnitImages() {
    // Given
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();
    String imageUrl = "http://localhost:8083/units/image.jpg";

    var mockImage = mock(io.github.kwatera_project.kwatera.property_service.model.UnitImage.class);
    when(mockImage.getId()).thenReturn(imageId);
    when(mockImage.getUrl()).thenReturn(imageUrl);
    when(mockImage.getIsMain()).thenReturn(true);

    when(service.getUnitImages(propertyId, unitId)).thenReturn(List.of(mockImage));

    // When
    var result = controller.getUnitImages(propertyId, unitId);

    // Then
    assertEquals(1, result.size());
    assertEquals(imageId, result.getFirst().getId());
    assertEquals(imageUrl, result.getFirst().getUrl());
    assertTrue(result.getFirst().getIsMain());

    verify(service, times(1)).getUnitImages(propertyId, unitId);
  }
}
