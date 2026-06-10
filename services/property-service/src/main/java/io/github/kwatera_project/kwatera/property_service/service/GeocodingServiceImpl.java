package io.github.kwatera_project.kwatera.property_service.service;

import io.github.kwatera_project.kwatera.property_service.dto.Coordinates;
import io.github.kwatera_project.kwatera.property_service.dto.NominatimResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeocodingServiceImpl implements GeocodingService {

  private final RestClient restClient;

  public GeocodingServiceImpl(RestClient.Builder builder) {
    this.restClient = builder.baseUrl("https://nominatim.openstreetmap.org").build();
  }

  @Override
  public Coordinates getCoordinates(
      String street, String streetNumber, String postalCode, String city, String country) {

    String query =
        String.format("%s %s, %s, %s %s", street, streetNumber, city, postalCode, country);

    List<NominatimResponse> result =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "jsonv2")
                        .build())
            .header("User-Agent", "kwatera-app")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    if (result == null || result.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unable to determine coordinates for address");
    }

    return new Coordinates(
        new BigDecimal(result.getFirst().lat()), new BigDecimal(result.getFirst().lon()));
  }
}
