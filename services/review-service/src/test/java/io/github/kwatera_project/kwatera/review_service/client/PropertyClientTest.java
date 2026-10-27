package io.github.kwatera_project.kwatera.review_service.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.review_service.client.PropertyClient.PropertyReference;
import io.github.kwatera_project.kwatera.review_service.client.PropertyClient.UnitReference;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PropertyClientTest {

  private static final String UNIT_URL = "http://property-service/api/properties/units/{id}";
  private static final String PROPERTY_URL = "http://property-service/api/properties/{id}";
  private static final String UNITS_URL = "http://property-service/api/properties/{id}/units";

  @Mock private RestOperations restOperations;

  private PropertyClient client;
  private final UUID propertyId = UUID.randomUUID();
  private final UUID unitId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    client = new PropertyClient(restOperations);
  }

  @Test
  void shouldAcceptExistingUnit() {
    when(restOperations.getForObject(UNIT_URL, UnitReference.class, unitId))
        .thenReturn(new UnitReference(unitId, propertyId));

    assertDoesNotThrow(() -> client.requireUnit(unitId));

    verify(restOperations).getForObject(UNIT_URL, UnitReference.class, unitId);
  }

  @ParameterizedTest
  @ValueSource(strings = {"body", "id"})
  void shouldRejectInvalidUnit(String field) {
    UnitReference unit =
        field.equals("body") ? null : new UnitReference(UUID.randomUUID(), propertyId);
    when(restOperations.getForObject(UNIT_URL, UnitReference.class, unitId)).thenReturn(unit);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> client.requireUnit(unitId));

    assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
  }

  @Test
  void shouldReturnDistinctUnitIdsInResponseOrder() {
    UUID secondUnitId = UUID.randomUUID();
    stubProperty();
    when(restOperations.getForObject(UNITS_URL, UnitReference[].class, propertyId))
        .thenReturn(
            new UnitReference[] {
              new UnitReference(unitId, propertyId),
              new UnitReference(secondUnitId, propertyId),
              new UnitReference(unitId, propertyId)
            });

    assertEquals(List.of(unitId, secondUnitId), client.getUnitIdsForProperty(propertyId));
  }

  @Test
  void shouldReturnEmptyListForPropertyWithoutUnits() {
    stubProperty();
    when(restOperations.getForObject(UNITS_URL, UnitReference[].class, propertyId))
        .thenReturn(new UnitReference[0]);

    assertTrue(client.getUnitIdsForProperty(propertyId).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"body", "id"})
  void shouldRejectInvalidPropertyBeforeLoadingUnits(String field) {
    PropertyReference property =
        field.equals("body") ? null : new PropertyReference(UUID.randomUUID());
    when(restOperations.getForObject(PROPERTY_URL, PropertyReference.class, propertyId))
        .thenReturn(property);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> client.getUnitIdsForProperty(propertyId));

    assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    verify(restOperations, never()).getForObject(UNITS_URL, UnitReference[].class, propertyId);
  }

  @ParameterizedTest
  @ValueSource(strings = {"body", "unit", "id", "property"})
  void shouldRejectInvalidUnitsResponse(String field) {
    stubProperty();
    UnitReference[] units =
        switch (field) {
          case "body" -> null;
          case "unit" -> new UnitReference[] {null};
          case "id" -> new UnitReference[] {new UnitReference(null, propertyId)};
          default -> new UnitReference[] {new UnitReference(unitId, UUID.randomUUID())};
        };
    when(restOperations.getForObject(UNITS_URL, UnitReference[].class, propertyId))
        .thenReturn(units);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> client.getUnitIdsForProperty(propertyId));

    assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"notFound", "connection", "response", "discovery"})
  void shouldTranslateServiceFailures(String failure) {
    RuntimeException cause =
        switch (failure) {
          case "notFound" ->
              HttpClientErrorException.create(
                  HttpStatus.NOT_FOUND, "Not found", HttpHeaders.EMPTY, new byte[0], null);
          case "connection" -> new ResourceAccessException("Connection refused");
          case "discovery" ->
              new IllegalStateException("No instances available for property-service");
          default -> new RestClientException("Invalid response");
        };
    HttpStatus expected =
        switch (failure) {
          case "notFound" -> HttpStatus.NOT_FOUND;
          case "connection", "discovery" -> HttpStatus.SERVICE_UNAVAILABLE;
          default -> HttpStatus.BAD_GATEWAY;
        };
    when(restOperations.getForObject(UNIT_URL, UnitReference.class, unitId)).thenThrow(cause);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> client.requireUnit(unitId));

    assertEquals(expected, exception.getStatusCode());
    assertSame(cause, exception.getCause());
  }

  @Test
  void shouldReturnNotFoundWhenPropertyDoesNotExist() {
    RuntimeException cause =
        HttpClientErrorException.create(
            HttpStatus.NOT_FOUND, "Not found", HttpHeaders.EMPTY, new byte[0], null);
    when(restOperations.getForObject(PROPERTY_URL, PropertyReference.class, propertyId))
        .thenThrow(cause);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> client.getUnitIdsForProperty(propertyId));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    verify(restOperations, never()).getForObject(UNITS_URL, UnitReference[].class, propertyId);
  }

  @Test
  void shouldPropagateUnexpectedIllegalState() {
    IllegalStateException cause = new IllegalStateException("Unexpected failure");
    when(restOperations.getForObject(UNIT_URL, UnitReference.class, unitId)).thenThrow(cause);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> client.requireUnit(unitId));

    assertSame(cause, exception);
  }

  private void stubProperty() {
    when(restOperations.getForObject(PROPERTY_URL, PropertyReference.class, propertyId))
        .thenReturn(new PropertyReference(propertyId));
  }
}
