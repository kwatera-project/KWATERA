package io.github.kwatera_project.kwatera.property_service.controller;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.property_service.service.PropertyService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OwnerPropertyControllerTest {

  @Mock private PropertyService propertyService;

  @Mock private Authentication authentication;

  @InjectMocks private OwnerPropertyController ownerPropertyController;

  private final UUID mockOwnerId = UUID.randomUUID();
  private final UUID propertyId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    lenient().when(authentication.isAuthenticated()).thenReturn(true);
  }

  @Test
  void getMyProperties_ShouldReturnList_WhenTokenDetailsAreValidUuid() {
    // Given
    PropertyDto dto =
        new PropertyDto(
            UUID.randomUUID(),
            mockOwnerId,
            "Test Property",
            "Desc",
            "Warsaw",
            null,
            null,
            null,
            "PL",
            "00-001",
            "Prosta",
            "1");
    when(authentication.getDetails()).thenReturn(mockOwnerId.toString());
    when(propertyService.getPropertiesByOwner(mockOwnerId)).thenReturn(List.of(dto));

    // When
    List<PropertyDto> result = ownerPropertyController.getMyProperties(authentication);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Test Property", result.get(0).getTitle());
    verify(propertyService).getPropertiesByOwner(mockOwnerId);
  }

  @Test
  void getMyProperties_ShouldReturn401_WhenTokenDetailsAreInvalidUuid() {
    // Given
    when(authentication.getDetails()).thenReturn("nie-poprawny-uuid-format");

    // When & Then
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> ownerPropertyController.getMyProperties(authentication));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    assertEquals("Unauthorized: Invalid token format", exception.getReason());
  }

  @Test
  void getUnits_ShouldPassParametersToService_WithDefaultCurrency() {
    // Given
    UnitDto unitDto =
        new UnitDto(
            UUID.randomUUID(),
            "Unit 1",
            "Desc",
            null,
            2,
            null,
            propertyId,
            null,
            "10",
            1,
            null,
            null);
    when(authentication.getDetails()).thenReturn(mockOwnerId.toString());
    when(propertyService.getUnitsForOwnerProperty(mockOwnerId, propertyId, "PLN"))
        .thenReturn(List.of(unitDto));

    // When
    List<UnitDto> result = ownerPropertyController.getUnits(propertyId, "PLN", authentication);

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Unit 1", result.get(0).getName());
    verify(propertyService).getUnitsForOwnerProperty(mockOwnerId, propertyId, "PLN");
  }

  @Test
  void getUnits_ShouldPassCustomCurrency_WhenProvidedInQuery() {
    // Given
    when(authentication.getDetails()).thenReturn(mockOwnerId.toString());
    when(propertyService.getUnitsForOwnerProperty(mockOwnerId, propertyId, "EUR"))
        .thenReturn(List.of());

    // When
    List<UnitDto> result = ownerPropertyController.getUnits(propertyId, "EUR", authentication);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(propertyService).getUnitsForOwnerProperty(mockOwnerId, propertyId, "EUR");
  }
}
