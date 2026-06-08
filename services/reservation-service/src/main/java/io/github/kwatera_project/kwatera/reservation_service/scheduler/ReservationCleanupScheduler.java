package io.github.kwatera_project.kwatera.reservation_service.scheduler;

import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationCleanupScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReservationCleanupScheduler.class);

  private final ReservationService reservationService;
  private final int expirationMinutes;

  public ReservationCleanupScheduler(
      ReservationService reservationService,
      @Value("${kwatera.reservation.cleanup.expiration-minutes:15}") int expirationMinutes) {
    this.reservationService = reservationService;
    this.expirationMinutes = expirationMinutes;
  }

  @Scheduled(cron = "${kwatera.reservation.cleanup.cron:0 */5 * * * *}")
  public void cleanupExpiredReservations() {
    log.info(
        "Starting scheduled reservation cleanup job. Expiration threshold: {} minutes",
        expirationMinutes);
    try {
      Instant threshold = Instant.now().minus(Duration.ofMinutes(expirationMinutes));
      reservationService.cancelExpiredPendingReservations(threshold);
      log.info("Scheduled reservation cleanup job completed successfully.");
    } catch (Exception e) {
      log.error("Error occurred during scheduled reservation cleanup job", e);
    }
  }
}
