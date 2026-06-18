package io.github.kwatera_project.kwatera.property_service.controller;

import io.github.kwatera_project.kwatera.property_service.dto.*;
import io.github.kwatera_project.kwatera.property_service.service.PropertyService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties")
public class PropertyController {

  private final PropertyService propertyService;

  @Value("${kwatera.security.internal-token:kwatera-internal-secret-token}")
  private String expectedInternalToken;

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

  @GetMapping("/internal/count")
  public long getPropertiesCount(
      @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
    validateInternalToken(internalToken);
    return propertyService.countAllProperties();
  }

  @GetMapping("/internal/owner-counts")
  public Map<UUID, Long> getOwnerPropertyCounts(
      @RequestParam(name = "ownerIds", required = false) List<UUID> ownerIds,
      @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
    validateInternalToken(internalToken);
    return propertyService.getOwnerPropertyCounts(ownerIds);
  }

  private void validateInternalToken(String internalToken) {
    if (internalToken == null || !internalToken.equals(expectedInternalToken)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Access denied: Invalid internal token");
    }
  }
}
