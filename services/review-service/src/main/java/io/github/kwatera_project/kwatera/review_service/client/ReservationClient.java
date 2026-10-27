package io.github.kwatera_project.kwatera.review_service.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.kwatera_project.kwatera.review_service.dto.ReservationDetailsDto;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ReservationClient {

  private final RestOperations restOperations;
  private final String internalToken;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "RestOperations is a shared Spring-managed dependency")
  public ReservationClient(
      RestOperations restOperations,
      @Value("${kwatera.security.internal-token:kwatera-internal-secret-token}")
          String internalToken) {
    this.restOperations = restOperations;
    this.internalToken = internalToken;
  }

  public ReservationDetailsDto getReservation(UUID reservationId) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Internal-Token", internalToken);

    try {
      ReservationDetailsDto reservation =
          restOperations
              .exchange(
                  "http://reservation-service/api/v1/reservations/internal/{id}",
                  HttpMethod.GET,
                  new HttpEntity<Void>(headers),
                  ReservationDetailsDto.class,
                  reservationId)
              .getBody();

      if (reservation == null
          || !reservationId.equals(reservation.id())
          || reservation.userId() == null
          || reservation.unitId() == null
          || reservation.status() == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "Reservation service returned incomplete or inconsistent data");
      }

      return reservation;

    } catch (HttpClientErrorException.NotFound exception) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found", exception);

    } catch (ResourceAccessException exception) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Reservation service is unavailable", exception);

    } catch (RestClientException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Reservation service returned an invalid response", exception);

    } catch (IllegalStateException exception) {
      if ("No instances available for reservation-service".equals(exception.getMessage())) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "Reservation service is unavailable", exception);
      }

      throw exception;
    }
  }
}
