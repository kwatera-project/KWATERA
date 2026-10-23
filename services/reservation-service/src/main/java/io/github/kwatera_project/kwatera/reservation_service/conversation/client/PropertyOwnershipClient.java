package io.github.kwatera_project.kwatera.reservation_service.conversation.client;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PropertyOwnershipClient {

  private final RestOperations restOperations;

  public UUID getOwnerIdForUnit(UUID unitId) {
    try {
      UnitOwnershipResponse unit =
          restOperations.getForObject(
              "http://property-service/api/properties/units/{unitId}",
              UnitOwnershipResponse.class,
              unitId);
      if (unit == null || unit.propertyId() == null) {
        throw inconsistentPropertyData();
      }

      PropertyOwnershipResponse property =
          restOperations.getForObject(
              "http://property-service/api/properties/{propertyId}",
              PropertyOwnershipResponse.class,
              unit.propertyId());
      if (property == null || property.ownerId() == null) {
        throw inconsistentPropertyData();
      }
      return property.ownerId();
    } catch (HttpClientErrorException.NotFound exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Reservation references property data that no longer exists",
          exception);
    } catch (ResourceAccessException exception) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Property service is unavailable", exception);
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (RestClientException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Property service returned an invalid response", exception);
    }
  }

  private ResponseStatusException inconsistentPropertyData() {
    return new ResponseStatusException(
        HttpStatus.BAD_GATEWAY, "Property service returned incomplete ownership data");
  }

  record UnitOwnershipResponse(UUID propertyId) {}

  record PropertyOwnershipResponse(UUID ownerId) {}
}
