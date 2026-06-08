package io.github.kwatera_project.kwatera.reservation_service.scheduler;

import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpcomingReservationSchedulerTest {

  @Mock private ReservationService reservationService;

  private UpcomingReservationScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new UpcomingReservationScheduler(reservationService);
  }

  @Test
  void shouldInvokeNotificationServiceSuccessfully() {
    scheduler.notifyUpcomingReservations();
    verify(reservationService).notifyUpcomingReservations();
  }

  @Test
  void shouldHandleExceptionAndLogInsteadOfThrowing() {
    doThrow(new RuntimeException("Service failure"))
        .when(reservationService)
        .notifyUpcomingReservations();

    scheduler.notifyUpcomingReservations();

    verify(reservationService).notifyUpcomingReservations();
  }
}
