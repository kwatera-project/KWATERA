package io.github.kwatera_project.kwatera.property_service.service;

import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.property_service.model.Property;
import io.github.kwatera_project.kwatera.property_service.model.PropertyImage;
import io.github.kwatera_project.kwatera.property_service.model.Unit;
import io.github.kwatera_project.kwatera.property_service.model.UnitImage;
import io.github.kwatera_project.kwatera.property_service.repository.PropertyImageRepository;
import io.github.kwatera_project.kwatera.property_service.repository.PropertyRepository;
import io.github.kwatera_project.kwatera.property_service.repository.UnitImageRepository;
import io.github.kwatera_project.kwatera.property_service.repository.UnitRepository;
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

  public PropertyService(
      PropertyRepository propertyRepository,
      UnitRepository unitRepository,
      PropertyImageRepository propertyImageRepository,
      UnitImageRepository unitImageRepository) {
    this.propertyRepository = propertyRepository;
    this.unitRepository = unitRepository;
    this.propertyImageRepository = propertyImageRepository;
    this.unitImageRepository = unitImageRepository;
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
        propertyRepository.findByOwnerId(ownerId).stream()
            .map(Property::getId)
            .toList();

    if (propertyIds.isEmpty()) {
      return List.of();
    }

    return unitRepository.findByPropertyIdIn(propertyIds).stream()
        .map(Unit::getId)
        .toList();
  }

  public List<String> getPropertyImages(UUID propertyId) {
    return propertyImageRepository.findByPropertyId(propertyId).stream()
        .map(PropertyImage::getUrl)
        .toList();
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
}
