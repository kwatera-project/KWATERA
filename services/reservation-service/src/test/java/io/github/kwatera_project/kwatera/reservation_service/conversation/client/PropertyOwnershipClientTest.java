package io.github.kwatera_project.kwatera.reservation_service.conversation.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PropertyOwnershipClientTest {

  @Mock private RestOperations restOperations;

  private PropertyOwnershipClient client;

  @BeforeEach
  void setUp() {
    client = new PropertyOwnershipClient(restOperations);
  }

  @Test
  void shouldResolveOwnerThroughUnitAndPropertyCatalog() {
    UUID unitId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    when(restOperations.getForObject(
            "http://property-service/api/properties/units/{unitId}",
            PropertyOwnershipClient.UnitOwnershipResponse.class,
            unitId))
        .thenReturn(new PropertyOwnershipClient.UnitOwnershipResponse(propertyId));
    when(restOperations.getForObject(
            "http://property-service/api/properties/{propertyId}",
            PropertyOwnershipClient.PropertyOwnershipResponse.class,
            propertyId))
        .thenReturn(new PropertyOwnershipClient.PropertyOwnershipResponse(ownerId));

    assertThat(client.getOwnerIdForUnit(unitId)).isEqualTo(ownerId);
  }

  @Test
  void shouldMapUnavailablePropertyServiceTo503() {
    UUID unitId = UUID.randomUUID();
    when(restOperations.getForObject(
            "http://property-service/api/properties/units/{unitId}",
            PropertyOwnershipClient.UnitOwnershipResponse.class,
            unitId))
        .thenThrow(new ResourceAccessException("connection refused"));

    assertThatThrownBy(() -> client.getOwnerIdForUnit(unitId))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
  }

  @Test
  void shouldMapMissingLoadBalancerInstanceTo503() {
    UUID unitId = UUID.randomUUID();
    when(restOperations.getForObject(
            "http://property-service/api/properties/units/{unitId}",
            PropertyOwnershipClient.UnitOwnershipResponse.class,
            unitId))
        .thenThrow(new IllegalStateException("No instances available for property-service"));

    assertThatThrownBy(() -> client.getOwnerIdForUnit(unitId))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
  }

  @Test
  void shouldNotMaskUnrelatedIllegalStateException() {
    UUID unitId = UUID.randomUUID();
    IllegalStateException failure = new IllegalStateException("unrelated failure");
    when(restOperations.getForObject(
            "http://property-service/api/properties/units/{unitId}",
            PropertyOwnershipClient.UnitOwnershipResponse.class,
            unitId))
        .thenThrow(failure);

    assertThatThrownBy(() -> client.getOwnerIdForUnit(unitId)).isSameAs(failure);
  }

  @Test
  void shouldMapMissingCatalogDataTo502() {
    UUID unitId = UUID.randomUUID();
    HttpClientErrorException notFound =
        HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8);
    when(restOperations.getForObject(
            "http://property-service/api/properties/units/{unitId}",
            PropertyOwnershipClient.UnitOwnershipResponse.class,
            unitId))
        .thenThrow(notFound);

    assertThatThrownBy(() -> client.getOwnerIdForUnit(unitId))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
  }
}
