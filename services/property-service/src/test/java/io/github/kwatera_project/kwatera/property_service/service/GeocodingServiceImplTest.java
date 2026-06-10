package io.github.kwatera_project.kwatera.property_service.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.property_service.dto.Coordinates;
import io.github.kwatera_project.kwatera.property_service.dto.NominatimResponse;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class GeocodingServiceImplTest {

  private GeocodingServiceImpl geocodingService;
  private MockRestServiceServer mockServer;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    this.mockServer = MockRestServiceServer.bindTo(builder).build();
    this.geocodingService = new GeocodingServiceImpl(builder);
  }

  @Test
  void getCoordinates_ShouldReturnCoordinates_WhenAddressIsValid() throws JsonProcessingException {
    // Given
    String street = "Marszałkowska";
    String streetNumber = "100";
    String postalCode = "00-001";
    String city = "Warszawa";
    String country = "Poland";

    String expectedQuery = "Marszałkowska 100, Warszawa, 00-001 Poland";

    NominatimResponse mockResponse = new NominatimResponse("52.2297", "21.0122");
    List<NominatimResponse> responseList = List.of(mockResponse);
    String jsonResponse = objectMapper.writeValueAsString(responseList);

    String encodedQuery =
        URLEncoder.encode(expectedQuery, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("%2C", ",");

    mockServer
        .expect(
            requestTo(
                "https://nominatim.openstreetmap.org/search?q=" + encodedQuery + "&format=jsonv2"))
        .andExpect(header("User-Agent", "kwatera-app"))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // When
    Coordinates result =
        geocodingService.getCoordinates(street, streetNumber, postalCode, city, country);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.latitude()).isEqualTo(new BigDecimal("52.2297"));
    assertThat(result.longitude()).isEqualTo(new BigDecimal("21.0122"));

    mockServer.verify();
  }

  @Test
  void getCoordinates_ShouldThrowResponseStatusException_WhenResponseIsEmpty()
      throws JsonProcessingException {
    // Given
    String street = "Invalid";
    String streetNumber = "0";
    String postalCode = "00-000";
    String city = "EmptyCity";
    String country = "Nowhere";

    List<NominatimResponse> emptyList = Collections.emptyList();
    String jsonResponse = objectMapper.writeValueAsString(emptyList);

    mockServer
        .expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    // When & Then
    assertThatThrownBy(
            () -> geocodingService.getCoordinates(street, streetNumber, postalCode, city, country))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
        .hasMessageContaining("Unable to determine coordinates for address");

    mockServer.verify();
  }

  @Test
  void getCoordinates_ShouldThrowResponseStatusException_WhenResponseIsNull() {
    // Given
    String street = "Invalid";
    String streetNumber = "0";
    String postalCode = "00-000";
    String city = "EmptyCity";
    String country = "Nowhere";

    mockServer
        .expect(requestTo(org.hamcrest.Matchers.containsString("/search")))
        .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

    // When & Then
    assertThatThrownBy(
            () -> geocodingService.getCoordinates(street, streetNumber, postalCode, city, country))
        .isInstanceOf(ResponseStatusException.class)
        .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
        .hasMessageContaining("Unable to determine coordinates for address");

    mockServer.verify();
  }
}
