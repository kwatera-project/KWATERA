package io.github.kwatera_project.kwatera.billing_service.client;

import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ReservationClient {

  private final RestTemplate restTemplate;

  @Value("${kwatera.security.internal-token:kwatera-internal-secret-token}")
  private String internalToken;

  public ReservationDto getReservation(UUID reservationId) {

    String url = "http://reservation-service/api/v1/reservations/internal/" + reservationId;

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Internal-Token", internalToken);

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    return restTemplate.exchange(url, HttpMethod.GET, entity, ReservationDto.class).getBody();
  }
}
