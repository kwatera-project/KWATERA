package io.github.kwatera_project.kwatera.reservation_service.scheduler;

import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReservationCleanupSchedulerTest {

  @Test
  void shouldInvokeCleanupWithCorrectThreshold() {
    ReservationService reservationService = mock(ReservationService.class);
    int expirationMinutes = 15;
    ReservationCleanupScheduler scheduler =
        new ReservationCleanupScheduler(reservationService, expirationMinutes);

    scheduler.cleanupExpiredReservations();

    ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(reservationService).cancelExpiredPendingReservations(thresholdCaptor.capture());

    Instant captured = thresholdCaptor.getValue();
    Instant now = Instant.now();

    long diffInSeconds = now.getEpochSecond() - captured.getEpochSecond();
    org.junit.jupiter.api.Assertions.assertTrue(
        diffInSeconds >= 900 - 5 && diffInSeconds <= 900 + 5,
        "Expected threshold to be around 900 seconds in the past, but was "
            + diffInSeconds
            + " seconds.");
  }
}
