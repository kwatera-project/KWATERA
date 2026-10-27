package io.github.kwatera_project.kwatera.review_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PropertyClient {

  private final RestOperations restOperations;

  public PropertyClient(RestOperations restOperations) {
    this.restOperations = restOperations;
  }

  public void requireUnit(UUID unitId) {
    UnitReference unit =
        get("http://property-service/api/properties/units/{id}", UnitReference.class, unitId);

    if (unit == null || !unitId.equals(unit.id())) {
      throw invalidResponse();
    }
  }

  public List<UUID> getUnitIdsForProperty(UUID propertyId) {
    PropertyReference property =
        get("http://property-service/api/properties/{id}", PropertyReference.class, propertyId);

    if (property == null || !propertyId.equals(property.id())) {
      throw invalidResponse();
    }

    UnitReference[] units =
        get("http://property-service/api/properties/{id}/units", UnitReference[].class, propertyId);

    if (units == null) {
      throw invalidResponse();
    }

    for (UnitReference unit : units) {
      if (unit == null || unit.id() == null || !propertyId.equals(unit.propertyId())) {
        throw invalidResponse();
      }
    }

    return Arrays.stream(units).map(UnitReference::id).distinct().toList();
  }

  private <T> T get(String url, Class<T> responseType, UUID id) {
    try {
      return restOperations.getForObject(url, responseType, id);

    } catch (HttpClientErrorException.NotFound exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found", exception);

    } catch (ResourceAccessException exception) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Property service is unavailable", exception);

    } catch (RestClientException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Property service returned an invalid response", exception);

    } catch (IllegalStateException exception) {
      if ("No instances available for property-service".equals(exception.getMessage())) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "Property service is unavailable", exception);
      }

      throw exception;
    }
  }

  private ResponseStatusException invalidResponse() {
    return new ResponseStatusException(
        HttpStatus.BAD_GATEWAY, "Property service returned incomplete or inconsistent data");
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record UnitReference(UUID id, UUID propertyId) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PropertyReference(UUID id) {}
}
