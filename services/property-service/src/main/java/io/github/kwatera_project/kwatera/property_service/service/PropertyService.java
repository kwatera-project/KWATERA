package io.github.kwatera_project.kwatera.property_service.service;

import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitSettlementItemDto;
import io.github.kwatera_project.kwatera.property_service.model.*;
import io.github.kwatera_project.kwatera.property_service.repository.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final PropertyImageRepository propertyImageRepository;
  private final UnitImageRepository unitImageRepository;
  private final UnitSettlementItemRepository unitSettlementItemRepository;

  public PropertyService(
      PropertyRepository propertyRepository,
      UnitRepository unitRepository,
      PropertyImageRepository propertyImageRepository,
      UnitImageRepository unitImageRepository,
      UnitSettlementItemRepository unitSettlementItemRepository) {
    this.propertyRepository = propertyRepository;
    this.unitRepository = unitRepository;
    this.propertyImageRepository = propertyImageRepository;
    this.unitImageRepository = unitImageRepository;
    this.unitSettlementItemRepository = unitSettlementItemRepository;
  }

  public List<UnitDto> getUnits(UUID propertyId) {
    if (!propertyRepository.existsById(propertyId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
    }
    return unitRepository.findByPropertyId(propertyId).stream().map(this::mapToDto).toList();
  }

  public List<PropertyDto> getAll() {
    return propertyRepository.findAll().stream().map(this::mapToDto).toList();
  }

  public PropertyDto getById(UUID id) {
    Property property =
        propertyRepository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));

    return mapToDto(property);
  }

  public UnitDto getUnitById(UUID id) {
    Unit unit =
        unitRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unit not found"));

    return mapToDto(unit);
  }

  public List<UUID> getUnitIdsByOwnerId(UUID ownerId) {
    List<UUID> propertyIds =
        propertyRepository.findByOwnerId(ownerId).stream().map(Property::getId).toList();

    if (propertyIds.isEmpty()) {
      return List.of();
    }

    return unitRepository.findByPropertyIdIn(propertyIds).stream().map(Unit::getId).toList();
  }

  public List<UUID> getAllUnitIds() {
    return unitRepository.findAll().stream().map(Unit::getId).toList();
  }

  public List<String> getPropertyImages(UUID propertyId) {
    return propertyImageRepository.findByPropertyId(propertyId).stream()
        .map(PropertyImage::getUrl)
        .toList();
  }

  public List<UnitSettlementItemDto> getUnitSettlementItems(UUID unitId) {

    List<UnitSettlementItem> items = unitSettlementItemRepository.findByUnitId(unitId);

    return items.stream().map(this::mapToDto).toList();
  }

  private PropertyDto mapToDto(Property property) {
    String imageUrl =
        propertyImageRepository
            .findByPropertyIdAndIsMainTrue(property.getId())
            .map(PropertyImage::getUrl)
            .orElse(null);

    return new PropertyDto(
        property.getId(),
        property.getTitle(),
        property.getDescription(),
        property.getLocation(),
        imageUrl);
  }

  private UnitDto mapToDto(Unit unit) {

    String imageUrl =
        unitImageRepository
            .findByUnitIdAndIsMainTrue(unit.getId())
            .map(UnitImage::getUrl)
            .orElse(null);

    return new UnitDto(
        unit.getId(),
        unit.getName(),
        unit.getDescription(),
        unit.getPricePerNight(),
        unit.getCapacity(),
        imageUrl);
  }

  private UnitSettlementItemDto mapToDto(UnitSettlementItem item) {
    return new UnitSettlementItemDto(
        item.getId(),
        item.getUnitId(),
        item.getSettlementItemType(),
        item.getPricePerUnit(),
        item.getMeasurementUnit(),
        item.getBillingType());
  }
}
