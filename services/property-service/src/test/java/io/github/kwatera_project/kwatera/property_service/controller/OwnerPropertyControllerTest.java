package io.github.kwatera_project.kwatera.property_service.controller;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.property_service.dto.PropertyCreateRequest;
import io.github.kwatera_project.kwatera.property_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.property_service.dto.PropertyUpdateRequest;
import io.github.kwatera_project.kwatera.property_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.property_service.service.PropertyService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OwnerPropertyControllerTest {

  @Mock private PropertyService propertyService;

  @Mock private Authentication authentication;

  @InjectMocks private OwnerPropertyController ownerPropertyController;

  private final UUID mockOwnerId = UUID.randomUUID();
  private final UUID propertyId = UUID.randomUUID();

  @Value("${app.file-server-url}")
  private String fileServerUrl;

  @BeforeEach
  void setUp() {
    lenient().when(authentication.isAuthenticated()).thenReturn(true);
  }

  Authentication auth(UUID userId, String token, boolean authenticated) {
    Authentication authentication = mock(Authentication.class);

    when(authentication.isAuthenticated()).thenReturn(authenticated);
    when(authentication.getPrincipal()).thenReturn(userId.toString());
    when(authentication.getDetails()).thenReturn(token);

    return authentication;
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
            "1",
            List.of(),
            null);

    when(authentication.getPrincipal()).thenReturn(mockOwnerId.toString());
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
    when(authentication.getPrincipal()).thenReturn("nie-poprawny-uuid-format");

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
            null,
            List.of(),
            null,
            null);

    when(authentication.getPrincipal()).thenReturn(mockOwnerId.toString());
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
    when(authentication.getPrincipal()).thenReturn(mockOwnerId.toString());
    when(propertyService.getUnitsForOwnerProperty(mockOwnerId, propertyId, "EUR"))
        .thenReturn(List.of());

    // When
    List<UnitDto> result = ownerPropertyController.getUnits(propertyId, "EUR", authentication);

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(propertyService).getUnitsForOwnerProperty(mockOwnerId, propertyId, "EUR");
  }

  @Test
  void deleteProperty_shouldThrow401_whenAuthenticationNull() {
    UUID propertyId = UUID.randomUUID();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> ownerPropertyController.deleteProperty(propertyId, null));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertTrue(ex.getReason().contains("Token is missing"));
  }

  @Test
  void deleteProperty_shouldThrow401_whenNotAuthenticated() {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(false);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> ownerPropertyController.deleteProperty(UUID.randomUUID(), auth));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void deleteProperty_shouldThrow401_whenDetailsMissing() {
    Authentication auth = auth(UUID.randomUUID(), null, true);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> ownerPropertyController.deleteProperty(UUID.randomUUID(), auth));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void deleteProperty_shouldThrow401_whenTokenEmpty() {
    Authentication auth = auth(UUID.randomUUID(), "   ", true);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> ownerPropertyController.deleteProperty(UUID.randomUUID(), auth));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void deleteProperty_shouldThrow401_whenInvalidUUID() {
    UUID propertyId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    String invalidToken = "not-a-uuid";

    Authentication auth = auth(ownerId, invalidToken, true);

    doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token format"))
        .when(propertyService)
        .deleteProperty(eq(ownerId), eq(propertyId), eq(invalidToken));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> ownerPropertyController.deleteProperty(propertyId, auth));

    assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    assertTrue(ex.getReason().contains("Invalid token format"));
  }

  @Test
  void deleteProperty_shouldCallService_whenValidToken() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    String token = UUID.randomUUID().toString();

    Authentication auth = auth(ownerId, token, true);

    ownerPropertyController.deleteProperty(propertyId, auth);

    verify(propertyService).deleteProperty(ownerId, propertyId, token);
  }

  @Test
  void createProperty_shouldCallService() {
    UUID ownerId = UUID.randomUUID();

    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getPrincipal()).thenReturn(ownerId.toString());

    PropertyCreateRequest request = mock(PropertyCreateRequest.class);

    when(propertyService.createProperty(any(), any())).thenReturn(mock(PropertyDto.class));

    ownerPropertyController.createProperty(request, auth);

    verify(propertyService).createProperty(eq(ownerId), eq(request));
  }

  @Test
  void updateProperty_shouldCallService() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();

    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getPrincipal()).thenReturn(ownerId.toString());

    PropertyUpdateRequest request = mock(PropertyUpdateRequest.class);

    when(propertyService.updateProperty(any(), any(), any())).thenReturn(mock(PropertyDto.class));

    ownerPropertyController.updateProperty(propertyId, request, auth);

    verify(propertyService).updateProperty(ownerId, propertyId, request);
  }

  @Test
  void deleteUnit_shouldCallService_withToken() {
    UUID ownerId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    String token = UUID.randomUUID().toString();

    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getDetails()).thenReturn(token);
    when(auth.getPrincipal()).thenReturn(ownerId.toString());

    // When
    ownerPropertyController.deleteUnit(unitId, propertyId, auth);

    // Then
    verify(propertyService, times(1)).deleteUnit(ownerId, propertyId, unitId, token);
  }

  @Test
  void uploadPropertyImage_ShouldCallService_WhenValidRequest() throws IOException {
    // Given
    UUID propertyId = UUID.randomUUID();
    MultipartFile mockFile =
        new MockMultipartFile("file", "image.jpg", "image/jpeg", "data".getBytes());

    when(authentication.getPrincipal()).thenReturn(mockOwnerId.toString());

    // When
    ownerPropertyController.uploadPropertyImage(propertyId, mockFile, true, authentication);

    // Then
    verify(propertyService).uploadPropertyImage(mockOwnerId, propertyId, true, mockFile);
  }

  @Test
  void deletePropertyImage_ShouldCallService_WhenValidRequest() throws IOException {
    // Given
    UUID propertyId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    when(authentication.getPrincipal()).thenReturn(mockOwnerId.toString());

    // When
    ownerPropertyController.deletePropertyImage(propertyId, imageId, authentication);

    // Then
    verify(propertyService).deletePropertyImage(mockOwnerId, propertyId, imageId);
  }

  @Test
  void setPropertyImageIsMain_ShouldCallService_WhenValidRequest() {
    // Given
    UUID propertyId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    when(authentication.getPrincipal()).thenReturn(mockOwnerId.toString());

    // When
    ownerPropertyController.setPropertyImageIsMain(propertyId, imageId, true, authentication);

    // Then
    verify(propertyService).setPropertyImageAsMain(mockOwnerId, propertyId, imageId, true);
  }

  @Test
  void uploadUnitImage_ShouldCallService_WhenValidRequest() throws IOException {
    // Given
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    MultipartFile mockFile =
        new MockMultipartFile("file", "room.png", "image/png", "data".getBytes());

    when(authentication.getPrincipal()).thenReturn(mockOwnerId.toString());

    // When
    ownerPropertyController.uploadUnitImage(propertyId, unitId, mockFile, false, authentication);

    // Then
    verify(propertyService).uploadUnitImage(mockOwnerId, propertyId, unitId, false, mockFile);
  }

  @Test
  void deleteUnitImage_ShouldCallService_WhenValidRequest() throws IOException {
    // Given
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    when(authentication.getPrincipal()).thenReturn(mockOwnerId.toString());

    // When
    ownerPropertyController.deleteUnitImage(propertyId, unitId, imageId, authentication);

    // Then
    verify(propertyService).deleteUnitImage(mockOwnerId, propertyId, unitId, imageId);
  }

  @Test
  void setUnitImageIsMain_ShouldCallService_WhenValidRequest() {
    // Given
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();

    when(authentication.getPrincipal()).thenReturn(mockOwnerId.toString());

    // When
    ownerPropertyController.setUnitImageIsMain(propertyId, unitId, imageId, true, authentication);

    // Then
    verify(propertyService).setUnitImageAsMain(mockOwnerId, propertyId, unitId, imageId, true);
  }
}
