package io.github.kwatera_project.kwatera.review_service.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.review_service.dto.ReservationDetailsDto;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReservationClientTest {

  private static final String URL = "http://reservation-service/api/v1/reservations/internal/{id}";

  @Mock private RestOperations restOperations;

  private ReservationClient client;
  private final UUID reservationId = UUID.randomUUID();
  private final UUID guestId = UUID.randomUUID();
  private final UUID unitId = UUID.randomUUID();
  private final LocalDate endDate = LocalDate.of(2026, 10, 27);

  @BeforeEach
  void setUp() {
    client = new ReservationClient(restOperations, "test-internal-token");
  }

  @Test
  void shouldReturnReservationAndSendInternalToken() {
    ReservationDetailsDto reservation =
        new ReservationDetailsDto(reservationId, guestId, unitId, "COMPLETED", endDate);
    stubResponse(reservation);

    assertSame(reservation, client.getReservation(reservationId));

    verify(restOperations)
        .exchange(
            eq(URL),
            eq(HttpMethod.GET),
            argThat(
                (HttpEntity<?> entity) ->
                    "test-internal-token".equals(entity.getHeaders().getFirst("X-Internal-Token"))),
            eq(ReservationDetailsDto.class),
            eq(reservationId));
  }

  @ParameterizedTest
  @ValueSource(strings = {"body", "id", "guest", "unit", "status"})
  void shouldRejectIncompleteOrInconsistentResponse(String missingField) {
    ReservationDetailsDto reservation =
        switch (missingField) {
          case "body" -> null;
          case "id" ->
              new ReservationDetailsDto(UUID.randomUUID(), guestId, unitId, "COMPLETED", endDate);
          case "guest" ->
              new ReservationDetailsDto(reservationId, null, unitId, "COMPLETED", endDate);
          case "unit" ->
              new ReservationDetailsDto(reservationId, guestId, null, "COMPLETED", endDate);
          default -> new ReservationDetailsDto(reservationId, guestId, unitId, null, endDate);
        };
    stubResponse(reservation);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> client.getReservation(reservationId));

    assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"notFound", "connection", "response", "discovery"})
  void shouldTranslateServiceFailures(String failure) {
    RuntimeException cause =
        switch (failure) {
          case "notFound" ->
              HttpClientErrorException.create(
                  HttpStatus.NOT_FOUND, "Not found", HttpHeaders.EMPTY, new byte[0], null);
          case "connection" -> new ResourceAccessException("Connection refused");
          case "discovery" ->
              new IllegalStateException("No instances available for reservation-service");
          default -> new RestClientException("Invalid response");
        };
    HttpStatus expected =
        switch (failure) {
          case "notFound" -> HttpStatus.NOT_FOUND;
          case "connection", "discovery" -> HttpStatus.SERVICE_UNAVAILABLE;
          default -> HttpStatus.BAD_GATEWAY;
        };
    stubFailure(cause);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> client.getReservation(reservationId));

    assertEquals(expected, exception.getStatusCode());
    assertSame(cause, exception.getCause());
  }

  @Test
  void shouldPropagateUnexpectedIllegalState() {
    IllegalStateException cause = new IllegalStateException("Unexpected failure");
    stubFailure(cause);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> client.getReservation(reservationId));

    assertSame(cause, exception);
  }

  private void stubResponse(ReservationDetailsDto reservation) {
    when(restOperations.exchange(
            eq(URL),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ReservationDetailsDto.class),
            eq(reservationId)))
        .thenReturn(ResponseEntity.ok(reservation));
  }

  private void stubFailure(RuntimeException cause) {
    when(restOperations.exchange(
            eq(URL),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ReservationDetailsDto.class),
            eq(reservationId)))
        .thenThrow(cause);
  }
}
