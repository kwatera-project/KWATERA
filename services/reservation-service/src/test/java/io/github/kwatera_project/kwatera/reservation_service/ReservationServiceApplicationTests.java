package io.github.kwatera_project.kwatera.reservation_service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.github.kwatera_project.kwatera.reservation_service.config.JpaConfig;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaConfig.class)
class ReservationServiceApplicationTests {

  @Autowired private TestEntityManager entityManager;

  @Test
  void shouldPersistAndLoadReservation() {
    // Given
    Reservation reservation = new Reservation();
    reservation.setUserId(UUID.randomUUID());
    reservation.setUnitId(UUID.randomUUID());
    reservation.setStartDate(LocalDate.now());
    reservation.setEndDate(LocalDate.now().plusDays(1));
    reservation.setStatus(ReservationStatus.PENDING);

    // When
    Reservation saved = entityManager.persistAndFlush(reservation);

    // Then
    Reservation found = entityManager.find(Reservation.class, saved.getId());

    assertThat(found).isNotNull();
    assertThat(found.getStatus()).isEqualTo(ReservationStatus.PENDING);
    assertThat(found.getCreatedAt()).isNotNull();
  }
}
