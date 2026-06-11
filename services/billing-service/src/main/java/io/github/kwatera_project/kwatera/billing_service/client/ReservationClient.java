package io.github.kwatera_project.kwatera.billing_service.client;

import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ReservationClient {

  private final RestTemplate restTemplate;

  public ReservationDto getReservation(UUID reservationId) {
    String url = "http://reservation-service/api/v1/reservations/" + reservationId;
    return restTemplate.getForObject(url, ReservationDto.class);
  }
}
