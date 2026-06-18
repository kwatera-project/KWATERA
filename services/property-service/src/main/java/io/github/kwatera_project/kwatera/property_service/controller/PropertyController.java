package io.github.kwatera_project.kwatera.property_service.controller;

import io.github.kwatera_project.kwatera.property_service.dto.*;
import io.github.kwatera_project.kwatera.property_service.service.PropertyService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties")
public class PropertyController {

  private final PropertyService propertyService;

  @GetMapping
  public List<PropertyDto> getAllProperties(
      @RequestParam(name = "minLat", required = false) BigDecimal minLat,
      @RequestParam(name = "maxLat", required = false) BigDecimal maxLat,
      @RequestParam(name = "minLng", required = false) BigDecimal minLng,
      @RequestParam(name = "maxLng", required = false) BigDecimal maxLng,
      @RequestParam(name = "amenities", required = false) List<String> amenities) {
    if (minLat != null && maxLat != null && minLng != null && maxLng != null) {
      return propertyService.getByBoundingBox(minLat, maxLat, minLng, maxLng, amenities);
    }
    return propertyService.getAll(amenities);
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
  public List<PropertyImageDto> getPropertyImages(@PathVariable("id") UUID id) {
    return propertyService.getPropertyImages(id).stream()
        .map(img -> new PropertyImageDto(img.getId(), img.getUrl(), img.getIsMain()))
        .toList();
  }

  @GetMapping("/{propertyId}/units/{unitId}/images")
  public List<UnitImageDto> getUnitImages(
      @PathVariable("propertyId") UUID propertyId, @PathVariable("unitId") UUID unitId) {
    return propertyService.getUnitImages(propertyId, unitId).stream()
        .map(img -> new UnitImageDto(img.getId(), img.getUrl(), img.getIsMain()))
        .toList();
  }

  @GetMapping("/units/{id}/settlement-items")
  public List<UnitSettlementItemDto> getUnitSettlementItems(@PathVariable("id") UUID id) {
    return propertyService.getUnitSettlementItems(id);
  }
}
