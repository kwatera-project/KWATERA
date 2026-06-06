package io.github.kwatera_project.kwatera.property_service.controller;

import io.github.kwatera_project.kwatera.property_service.dto.*;
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
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerPropertyController {

  private final PropertyService propertyService;

  @GetMapping("/properties")
  @PreAuthorize("hasRole('ROLE_OWNER')")
  public List<PropertyDto> getMyProperties(Authentication authentication) {

      if (authentication != null) {
          System.out.println("Principal class: " + authentication.getPrincipal().getClass().getName());
          System.out.println("Principal value: " + authentication.getPrincipal());
      } else {
          System.out.println("Authentication is NULL!");
      }

    UUID ownerId = validateAndGetUserId(authentication);

    return propertyService.getPropertiesByOwner(ownerId);
  }

  @GetMapping("/property/{propertyId}/units")
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
    Object principal = authentication.getPrincipal();
    if (!(principal instanceof String) || ((String) principal).trim().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }
    try {
      return UUID.fromString((String) principal);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid token format");
    }
  }

  private String validateAndGetToken(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Token is missing");
    }
    Object details = authentication.getDetails();
    if (!(details instanceof String) || ((String) details).trim().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }
    try {
        return (String) details;
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid token format");
    }
  }

  @PostMapping("/property")
  @PreAuthorize("hasRole('ROLE_OWNER')")
  public PropertyDto createProperty(
      @RequestBody PropertyCreateRequest request, Authentication authentication) {

    UUID ownerId = validateAndGetUserId(authentication);

    return propertyService.createProperty(ownerId, request);
  }

  @PatchMapping("/property/{propertyId}")
  @PreAuthorize("hasRole('ROLE_OWNER')")
  public PropertyDto updateProperty(
      @PathVariable("propertyId") UUID propertyId,
      @RequestBody PropertyUpdateRequest request,
      Authentication authentication) {

    UUID ownerId = validateAndGetUserId(authentication);

    return propertyService.updateProperty(ownerId, propertyId, request);
  }

  @DeleteMapping("/property/{propertyId}")
  @PreAuthorize("hasRole('ROLE_OWNER')")
  public void deleteProperty(
      @PathVariable("propertyId") UUID propertyId, Authentication authentication) {

    UUID ownerId = validateAndGetUserId(authentication);
    String token = validateAndGetToken(authentication);

    propertyService.deleteProperty(ownerId, propertyId, token);
  }

  @PostMapping("/property/{propertyId}/units")
  @PreAuthorize("hasRole('ROLE_OWNER')")
  public UnitDto createUnit(
      @PathVariable("propertyId") UUID propertyId,
      @RequestBody UnitCreateRequest request,
      Authentication authentication) {

    UUID ownerId = validateAndGetUserId(authentication);

    return propertyService.createUnit(ownerId, propertyId, request);
  }

  @PatchMapping("/property/{propertyId}/unit/{unitId}")
  @PreAuthorize("hasRole('ROLE_OWNER')")
  public UnitDto updateUnit(
      @PathVariable("unitId") UUID unitId,
      @PathVariable("propertyId") UUID propertyId,
      @RequestParam(name = "currency", defaultValue = "PLN") String currency,
      @RequestBody UnitUpdateRequest request,
      Authentication authentication) {

    UUID ownerId = validateAndGetUserId(authentication);

    return propertyService.updateUnit(ownerId, propertyId, unitId, currency, request);
  }

  @DeleteMapping("/property/{propertyId}/unit/{unitId}")
  @PreAuthorize("hasRole('ROLE_OWNER')")
  public void deleteUnit(
      @PathVariable("unitId") UUID unitId,
      @PathVariable("propertyId") UUID propertyId,
      Authentication authentication) {

    UUID ownerId = validateAndGetUserId(authentication);
    String token = validateAndGetToken(authentication);

    propertyService.deleteUnit(ownerId, propertyId, unitId, token);
  }
}
