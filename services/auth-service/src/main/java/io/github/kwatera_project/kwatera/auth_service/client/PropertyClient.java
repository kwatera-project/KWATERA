package io.github.kwatera_project.kwatera.auth_service.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class PropertyClient {

  private final RestTemplate restTemplate;

  @Value("${kwatera.security.internal-token:kwatera-internal-secret-token}")
  private String internalToken;

  @Value("${services.property.url:http://property-service/api/properties}")
  private String propertyServiceUrl;

  public long getTotalPropertiesCount() {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Internal-Token", internalToken);
      HttpEntity<Void> entity = new HttpEntity<>(headers);

      String url = propertyServiceUrl + "/internal/count";
      ResponseEntity<Long> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, Long.class);
      return response.getBody() != null ? response.getBody() : 0L;
    } catch (Exception e) {
      log.error("Failed to fetch total properties count from property-service", e);
      return 0L;
    }
  }

  public Map<UUID, Long> getOwnerPropertyCounts(List<UUID> ownerIds) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return Collections.emptyMap();
    }
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Internal-Token", internalToken);
      HttpEntity<Void> entity = new HttpEntity<>(headers);

      String ownerIdsStr = ownerIds.stream().map(UUID::toString).collect(Collectors.joining(","));

      String url =
          UriComponentsBuilder.fromUriString(propertyServiceUrl + "/internal/owner-counts")
              .queryParam("ownerIds", ownerIdsStr)
              .toUriString();

      ResponseEntity<Map<UUID, Long>> response =
          restTemplate.exchange(
              url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<UUID, Long>>() {});
      return response.getBody() != null ? response.getBody() : Collections.emptyMap();
    } catch (Exception e) {
      log.error("Failed to fetch owner property counts from property-service", e);
      return Collections.emptyMap();
    }
  }

  public List<Map<String, Object>> getRandomProperties(int count) {
    try {
      ResponseEntity<List<Map<String, Object>>> response =
          restTemplate.exchange(
              propertyServiceUrl,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<Map<String, Object>>>() {});
      List<Map<String, Object>> list = response.getBody();
      if (list == null || list.isEmpty()) {
        return Collections.emptyList();
      }
      List<Map<String, Object>> mutableList = new java.util.ArrayList<>(list);
      Collections.shuffle(mutableList);
      return mutableList.stream().limit(count).collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Failed to fetch random properties from property-service", e);
      return Collections.emptyList();
    }
  }

  public List<Map<String, Object>> getPropertyUnits(UUID propertyId) {
    if (propertyId == null) {
      return Collections.emptyList();
    }
    try {
      String url = propertyServiceUrl + "/" + propertyId + "/units";
      ResponseEntity<List<Map<String, Object>>> response =
          restTemplate.exchange(
              url,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<Map<String, Object>>>() {});
      return response.getBody() != null ? response.getBody() : Collections.emptyList();
    } catch (Exception e) {
      log.error("Failed to fetch units for property {}", propertyId, e);
      return Collections.emptyList();
    }
  }
}
