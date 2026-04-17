package io.github.kwatera_project.kwatera.reservation_service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.github.kwatera_project.kwatera.reservation_service.config.JpaConfig;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaConfig.class)
class ReservationServiceApplicationTests {

  @Autowired private TestEntityManager entityManager;

  @Test
  void shouldPersistAndLoadReservation() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now().plusDays(5);

    Reservation reservation = new Reservation();
    reservation.setUserId(userId);
    reservation.setUnitId(unitId);
    reservation.setStartDate(start);
    reservation.setEndDate(end);
    reservation.setStatus(ReservationStatus.PENDING);

    // When
    Reservation saved = entityManager.persistAndFlush(reservation);
    entityManager.clear();

    // Then
    Reservation found = entityManager.find(Reservation.class, saved.getId());

    assertThat(found).isNotNull();
    assertThat(found.getUserId()).isEqualTo(userId);
    assertThat(found.getUnitId()).isEqualTo(unitId);
    assertThat(found.getStartDate()).isEqualTo(start);
    assertThat(found.getEndDate()).isEqualTo(end);
    assertThat(found.getStatus()).isEqualTo(ReservationStatus.PENDING);
    assertThat(found.getCreatedAt()).isNotNull();
  }

  @Test
  void shouldUpdateLastModifiedDate() throws InterruptedException {
    // Given
    Reservation reservation = new Reservation();
    reservation.setUserId(UUID.randomUUID());
    reservation.setUnitId(UUID.randomUUID());
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(1));
    reservation.setStatus(ReservationStatus.PENDING);

    Reservation saved = entityManager.persistAndFlush(reservation);
    Instant initialUpdateAt = saved.getUpdatedAt();

    // When - status change
    saved.setStatus(ReservationStatus.CONFIRMED);
    Thread.sleep(10);
    Reservation updated = entityManager.persistAndFlush(saved);

    // Then
    assertThat(updated.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    assertThat(updated.getUpdatedAt()).isAfter(initialUpdateAt);
  }

  @Test
  void shouldHandleAllReservationStatuses() {
    for (ReservationStatus status : ReservationStatus.values()) {
      Reservation res = new Reservation();
      res.setUserId(UUID.randomUUID());
      res.setUnitId(UUID.randomUUID());
      res.setStartDate(LocalDate.now());
      res.setEndDate(LocalDate.now().plusDays(1));
      res.setStatus(status);

      Reservation saved = entityManager.persistAndFlush(res);
      assertThat(saved.getStatus()).isEqualTo(status);
    }
  }
}
