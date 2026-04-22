package io.github.kwatera_project.kwatera.property_service.service;

import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.property_service.model.Property;
import io.github.kwatera_project.kwatera.property_service.model.Unit;
import io.github.kwatera_project.kwatera.property_service.repository.PropertyRepository;
import io.github.kwatera_project.kwatera.property_service.repository.UnitRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PropertyService {

  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;

  public PropertyService(PropertyRepository propertyRepository, UnitRepository unitRepository) {
    this.propertyRepository = propertyRepository;
    this.unitRepository = unitRepository;
  }

  public List<UnitDto> getUnits(UUID propertyId) {
    if (!propertyRepository.existsById(propertyId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
    }

    List<Unit> units = unitRepository.findByPropertyId(propertyId);
    List<UnitDto> result = new ArrayList<>();

    for (Unit unit : units) {
      result.add(mapToDto(unit));
    }

    return result;
  }

  public List<PropertyDto> getAll() {
    List<Property> properties = propertyRepository.findAll();
    List<PropertyDto> result = new ArrayList<>();
    for (Property property : properties) {
      result.add(mapToDto(property));
    }
    return result;
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

    private PropertyDto mapToDto(Property property) {
        return new PropertyDto(
                property.getId(),
                property.getTitle(),
                property.getDescription(),
                property.getLocation(),
                property.getImageUrl()
        );
    }

    private UnitDto mapToDto(Unit unit) {
        return new UnitDto(
                unit.getId(),
                unit.getName(),
                unit.getDescription(),
                unit.getPricePerNight(),
                unit.getCapacity()
        );
    }
}
