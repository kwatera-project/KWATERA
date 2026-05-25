package io.github.kwatera_project.kwatera.property_service.controller;

import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitSettlementItemDto;
import io.github.kwatera_project.kwatera.property_service.service.PropertyService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@RestController
@RequestMapping("/api/properties")
public class PropertyController {

  private final PropertyService propertyService;

  public PropertyController(PropertyService propertyService) {
    this.propertyService = propertyService;
  }

  @GetMapping
  public List<PropertyDto> getAllProperties() {
    return propertyService.getAll();
  }

  @GetMapping("/{id}")
  public PropertyDto getPropertyById(@PathVariable("id") UUID id) {
    return propertyService.getById(id);
  }

  @GetMapping("/{id}/units")
  public List<UnitDto> getUnits(
      @PathVariable("id") UUID id,
      @RequestParam(name = "currency", defaultValue = "PLN") String currency) {
    return propertyService.getUnits(id, currency);
  }

  @GetMapping("/units/{id}")
  public UnitDto getUnit(
      @PathVariable("id") UUID id,
      @RequestParam(name = "currency", defaultValue = "PLN") String currency) {
    return propertyService.getUnitById(id, currency);
  }

  @GetMapping("/units/ids")
  public List<UUID> getAllUnitIds() {
    return propertyService.getAllUnitIds();
  }

  @GetMapping("/units/ids/{ownerId}")
  public List<UUID> getUnitIdsByOwnerId(@PathVariable("ownerId") UUID ownerId) {
    return propertyService.getUnitIdsByOwnerId(ownerId);
  }

  @GetMapping("/{id}/images")
  public List<String> getPropertyImages(@PathVariable("id") UUID id) {
    return propertyService.getPropertyImages(id);
  }

  @GetMapping("/units/{id}/settlement-items")
  public List<UnitSettlementItemDto> getUnitSettlementItems(@PathVariable("id") UUID id) {
    return propertyService.getUnitSettlementItems(id);
  }
}
