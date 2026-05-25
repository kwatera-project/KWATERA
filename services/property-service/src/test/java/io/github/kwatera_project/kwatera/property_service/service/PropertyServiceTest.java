package io.github.kwatera_project.kwatera.property_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.property_service.dto.UnitSettlementItemDto;
import io.github.kwatera_project.kwatera.property_service.model.*;
import io.github.kwatera_project.kwatera.property_service.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PropertyServiceTest {

  private PropertyRepository propertyRepository;
  private UnitRepository unitRepository;
  private PropertyService propertyService;
  private PropertyImageRepository propertyImageRepository;
  private UnitImageRepository unitImageRepository;
  private UnitSettlementItemRepository unitSettlementItemRepository;
  private io.github.kwatera_project.kwatera.property_service.client.NbpExchangeRateClient nbpExchangeRateClient;

  @BeforeEach
  void setUp() {
    propertyRepository = mock(PropertyRepository.class);
    unitRepository = mock(UnitRepository.class);
    propertyImageRepository = mock(PropertyImageRepository.class);
    unitImageRepository = mock(UnitImageRepository.class);
    unitSettlementItemRepository = mock(UnitSettlementItemRepository.class);
    nbpExchangeRateClient = mock(io.github.kwatera_project.kwatera.property_service.client.NbpExchangeRateClient.class);

    propertyService =
        new PropertyService(
            propertyRepository,
            unitRepository,
            propertyImageRepository,
            unitImageRepository,
            unitSettlementItemRepository,
            nbpExchangeRateClient);
  }

  @Test
  void getAll_shouldReturnProperties() {
    Property property = new Property();
    property.setId(UUID.randomUUID());
    property.setTitle("Test");
    property.setLocation("Warsaw");
    property.setDescription("Desc");

    when(propertyRepository.findAll()).thenReturn(List.of(property));

    var result = propertyService.getAll();

    assertEquals(1, result.size());
    assertEquals("Test", result.get(0).getTitle());
  }

  @Test
  void getById_shouldReturnProperty() {
    UUID id = UUID.randomUUID();

    Property property = new Property();
    property.setId(id);
    property.setTitle("Test");
    property.setLocation("Warsaw");
    property.setDescription("Desc");

    when(propertyRepository.findById(id)).thenReturn(Optional.of(property));

    var result = propertyService.getById(id);

    assertEquals(id, result.getId());
    assertNull(result.getImageUrl());
  }

  @Test
  void getById_shouldThrowWhenNotFound() {
    UUID id = UUID.randomUUID();

    when(propertyRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> propertyService.getById(id));
  }

  @Test
  void getUnits_shouldReturnUnits() {
    UUID propertyId = UUID.randomUUID();

    when(propertyRepository.existsById(propertyId)).thenReturn(true);

    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());
    unit.setName("Room");
    unit.setDescription("Desc");
    unit.setCapacity(2);
    unit.setPricePerNight(BigDecimal.valueOf(200));

    when(unitRepository.findByPropertyId(propertyId)).thenReturn(List.of(unit));

    var result = propertyService.getUnits(propertyId, "PLN");

    assertEquals(1, result.size());
    assertEquals("Room", result.get(0).getName());
  }

  @Test
  void getUnits_shouldThrowWhenPropertyNotExists() {
    UUID propertyId = UUID.randomUUID();

    when(propertyRepository.existsById(propertyId)).thenReturn(false);

    assertThrows(ResponseStatusException.class, () -> propertyService.getUnits(propertyId, "PLN"));
  }

  @Test
  void getUnitById_shouldReturnUnit() {
    UUID id = UUID.randomUUID();

    Unit unit = new Unit();
    unit.setId(id);
    unit.setName("Room");
    unit.setDescription("Desc");
    unit.setCapacity(2);
    unit.setPricePerNight(BigDecimal.valueOf(200));

    when(unitRepository.findById(id)).thenReturn(Optional.of(unit));

    var result = propertyService.getUnitById(id, "PLN");

    assertEquals(id, result.getId());
  }

  @Test
  void getUnitById_shouldThrowWhenNotFound() {
    UUID id = UUID.randomUUID();

    when(unitRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> propertyService.getUnitById(id, "PLN"));
  }

  @Test
  void getUnitIdsByOwnerId_shouldReturnEmptyWhenNoProperties() {
    UUID ownerId = UUID.randomUUID();
    when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of());

    var result = propertyService.getUnitIdsByOwnerId(ownerId);

    assertTrue(result.isEmpty());
  }

  @Test
  void getUnitIdsByOwnerId_shouldReturnUnitIdsWhenPropertiesExist() {
    UUID ownerId = UUID.randomUUID();

    Property property = new Property();
    property.setId(UUID.randomUUID());

    Unit unit = new Unit();
    unit.setId(UUID.randomUUID());

    when(propertyRepository.findByOwnerId(ownerId)).thenReturn(List.of(property));
    when(unitRepository.findByPropertyIdIn(List.of(property.getId()))).thenReturn(List.of(unit));

    var result = propertyService.getUnitIdsByOwnerId(ownerId);

    assertEquals(1, result.size());
    assertEquals(unit.getId(), result.get(0));
  }

  @Test
  void getPropertyImages_shouldReturnImages() {
    UUID propertyId = UUID.randomUUID();

    io.github.kwatera_project.kwatera.property_service.model.PropertyImage img1 =
        new io.github.kwatera_project.kwatera.property_service.model.PropertyImage();
    img1.setUrl("img1.jpg");

    when(propertyImageRepository.findByPropertyId(propertyId)).thenReturn(List.of(img1));

    var result = propertyService.getPropertyImages(propertyId);

    assertEquals(1, result.size());
    assertEquals("img1.jpg", result.get(0));
  }

  @Test
  void getAllUnitIds_shouldReturnUnitIds() {
    Unit u1 = new Unit();
    u1.setId(UUID.randomUUID());

    Unit u2 = new Unit();
    u2.setId(UUID.randomUUID());

    when(unitRepository.findAll()).thenReturn(List.of(u1, u2));

    List<UUID> result = propertyService.getAllUnitIds();

    assertEquals(2, result.size());
    assertTrue(result.contains(u1.getId()));
    assertTrue(result.contains(u2.getId()));
  }

  @Test
  void getUnitSettlementItems_shouldReturnDtos() {
    UUID unitId = UUID.randomUUID();

    UnitSettlementItem item = new UnitSettlementItem();
    item.setId(UUID.randomUUID());
    item.setUnitId(unitId);
    item.setSettlementItemType(SettlementItemType.DEPOSIT);
    item.setPricePerUnit(BigDecimal.TEN);
    item.setMeasurementUnit(null);
    item.setBillingType(BillingType.FIXED);

    when(unitSettlementItemRepository.findByUnitId(unitId)).thenReturn(List.of(item));

    var result = propertyService.getUnitSettlementItems(unitId);

    assertEquals(1, result.size());

    UnitSettlementItemDto dto = result.get(0);
    assertEquals(item.getId(), dto.id());
    assertEquals(unitId, dto.unitId());
    assertEquals(SettlementItemType.DEPOSIT, dto.settlementItemType());
    assertEquals(BigDecimal.TEN, dto.pricePerUnit());
    assertNull(dto.measurementUnit());
    assertEquals(BillingType.FIXED, dto.billingType());
  }

  @Test
  void getUnitSettlementItems_shouldReturnEmptyList() {
    UUID unitId = UUID.randomUUID();

    when(unitSettlementItemRepository.findByUnitId(unitId)).thenReturn(List.of());

    var result = propertyService.getUnitSettlementItems(unitId);

    assertTrue(result.isEmpty());
  }
}
