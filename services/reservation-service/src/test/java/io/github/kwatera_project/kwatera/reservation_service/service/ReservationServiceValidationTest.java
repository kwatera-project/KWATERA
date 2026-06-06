package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ReservationServiceValidationTest {

  private final ReservationRepository repository = mock(ReservationRepository.class);
  private final ReservationService service =
      new ReservationService(
          repository,
          mock(org.springframework.web.client.RestTemplate.class),
          mock(
              io.github.kwatera_project.kwatera.reservation_service.client.NbpExchangeRateClient
                  .class),
          mock(EmailNotificationService.class),
          new BusinessDateProvider("Europe/Warsaw"));

  @Test
  void shouldThrow_whenDatesAreNull() {
    UUID unitId = UUID.randomUUID();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> service.checkAvailability(unitId, null, null));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Dates are required", ex.getReason());
  }

  @Test
  void shouldThrow_whenFromIsInPast() {
    LocalDate from = LocalDate.now().minusDays(1);
    LocalDate to = LocalDate.now().plusDays(2);

    UUID unitId = UUID.randomUUID();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> service.checkAvailability(unitId, from, to));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Date is in the past", ex.getReason());
  }

  @Test
  void shouldThrow_whenFromNotBeforeTo() {
    LocalDate from = LocalDate.now().plusDays(5);
    LocalDate to = LocalDate.now().plusDays(5);

    UUID unitId = UUID.randomUUID();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> service.checkAvailability(unitId, from, to));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Invalid date range", ex.getReason());
  }

  @Test
  void shouldThrow_whenUnitIdIsNull() {
    LocalDate from = LocalDate.now().plusDays(1);
    LocalDate to = LocalDate.now().plusDays(5);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> service.checkAvailability(null, from, to));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    assertEquals("Unit id is required", ex.getReason());
  }
}
