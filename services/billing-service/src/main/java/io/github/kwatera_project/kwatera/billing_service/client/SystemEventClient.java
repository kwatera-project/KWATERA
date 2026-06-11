package io.github.kwatera_project.kwatera.billing_service.client;

import io.github.kwatera_project.kwatera.billing_service.dto.InternalSystemEventRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class SystemEventClient {

  private static final Logger log = LoggerFactory.getLogger(SystemEventClient.class);
  private static final String INTERNAL_SYSTEM_EVENTS_URL =
      "http://reservation-service/api/v1/internal/system-events";

  private final RestTemplate restTemplate;

  @Value("${kwatera.security.internal-token:kwatera-internal-secret-token}")
  private String internalToken;

  public void logSafely(
      String actionType, UUID actorUserId, String entityType, UUID entityId, String details) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-Internal-Token", internalToken);
      InternalSystemEventRequest request =
          new InternalSystemEventRequest(actionType, actorUserId, entityType, entityId, details);
      restTemplate.exchange(
          INTERNAL_SYSTEM_EVENTS_URL,
          HttpMethod.POST,
          new HttpEntity<>(request, headers),
          Void.class);
    } catch (Exception e) {
      log.warn("Failed to publish system event {}", actionType, e);
    }
  }
}
