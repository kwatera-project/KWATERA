package io.github.kwatera_project.kwatera.auth_service.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class PropertyClientTest {

  @Mock private RestTemplate restTemplate;

  private PropertyClient propertyClient;

  @BeforeEach
  void setUp() {
    propertyClient = new PropertyClient(restTemplate);
    ReflectionTestUtils.setField(propertyClient, "internalToken", "test-token");
    ReflectionTestUtils.setField(
        propertyClient, "propertyServiceUrl", "http://property-service/api/properties");
  }

  @Test
  void shouldReturnTotalPropertiesCount() {
    when(restTemplate.exchange(
            eq("http://property-service/api/properties/internal/count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Long.class)))
        .thenReturn(new ResponseEntity<>(12L, HttpStatus.OK));

    long count = propertyClient.getTotalPropertiesCount();

    assertEquals(12L, count);
  }

  @Test
  void shouldReturnZeroCountOnException() {
    when(restTemplate.exchange(
            eq("http://property-service/api/properties/internal/count"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Long.class)))
        .thenThrow(new RestClientException("Failed"));

    long count = propertyClient.getTotalPropertiesCount();

    assertEquals(0L, count);
  }

  @Test
  void shouldReturnOwnerPropertyCounts() {
    UUID ownerId = UUID.randomUUID();
    Map<UUID, Long> mockResponse = Map.of(ownerId, 4L);

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

    Map<UUID, Long> counts = propertyClient.getOwnerPropertyCounts(List.of(ownerId));

    assertEquals(1, counts.size());
    assertEquals(4L, counts.get(ownerId));
  }

  @Test
  void shouldReturnEmptyMapOnException() {
    UUID ownerId = UUID.randomUUID();

    when(restTemplate.exchange(
            anyString(),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenThrow(new RestClientException("Failed"));

    Map<UUID, Long> counts = propertyClient.getOwnerPropertyCounts(List.of(ownerId));

    assertTrue(counts.isEmpty());
  }

  @Test
  void shouldReturnEmptyMapForNullOrEmptyList() {
    Map<UUID, Long> nullCounts = propertyClient.getOwnerPropertyCounts(null);
    Map<UUID, Long> emptyCounts = propertyClient.getOwnerPropertyCounts(Collections.emptyList());

    assertTrue(nullCounts.isEmpty());
    assertTrue(emptyCounts.isEmpty());
    verifyNoInteractions(restTemplate);
  }
}
