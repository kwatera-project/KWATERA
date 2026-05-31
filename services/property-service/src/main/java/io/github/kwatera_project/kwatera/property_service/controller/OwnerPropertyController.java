package io.github.kwatera_project.kwatera.property_service.controller;

import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.property_service.service.PropertyService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/owner/properties")
@RequiredArgsConstructor
public class OwnerPropertyController {

  private final PropertyService propertyService;

  @GetMapping
  @PreAuthorize("hasRole('ROLE_OWNER')")
  public List<PropertyDto> getMyProperties(Authentication authentication) {

    UUID ownerId = validateAndGetUserId(authentication);

    return propertyService.getPropertiesByOwner(ownerId);
  }

  @GetMapping("/{propertyId}/units")
  @PreAuthorize("hasRole('ROLE_OWNER')")
  public List<UnitDto> getUnits(
      @PathVariable("propertyId") UUID propertyId,
      @RequestParam(name = "currency", defaultValue = "PLN") String currency,
      Authentication authentication) {
    UUID ownerId = validateAndGetUserId(authentication);

    return propertyService.getUnitsForOwnerProperty(ownerId, propertyId, currency);
  }

  private UUID validateAndGetUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Token is missing");
    }
    Object details = authentication.getDetails();
    if (!(details instanceof String) || ((String) details).trim().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }
    try {
      return UUID.fromString((String) details);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid token format");
    }
  }
}
