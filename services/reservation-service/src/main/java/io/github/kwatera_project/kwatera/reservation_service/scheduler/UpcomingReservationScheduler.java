package io.github.kwatera_project.kwatera.reservation_service.scheduler;

import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UpcomingReservationScheduler {

  private static final Logger log = LoggerFactory.getLogger(UpcomingReservationScheduler.class);

  private final ReservationService reservationService;

  public UpcomingReservationScheduler(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

  @Scheduled(
      cron = "${kwatera.reservation.upcoming-check.cron:0 0 8 * * *}",
      zone = "${kwatera.reservation.upcoming-check.zone:Europe/Warsaw}")
  public void notifyUpcomingReservations() {
    log.info("Starting scheduled upcoming reservations check.");
    try {
      reservationService.notifyUpcomingReservations();
      log.info("Scheduled upcoming reservations check completed successfully.");
    } catch (Exception e) {
      log.error("Error occurred during scheduled upcoming reservations check", e);
    }
  }
}
