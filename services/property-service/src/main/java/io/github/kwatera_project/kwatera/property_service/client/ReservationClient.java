package io.github.kwatera_project.kwatera.property_service.client;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ReservationClient {

  private final RestClient reservationRestClient;

  public boolean hasReservationsForUnit(UUID unitId, String bearerToken) {
    return Boolean.TRUE.equals(
        reservationRestClient
            .get()
            .uri("/api/v1/reservations/units/{unitId}/has-reservations", unitId)
            .header(HttpHeaders.AUTHORIZATION, bearerToken)
            .retrieve()
            .body(Boolean.class));
  }
}
