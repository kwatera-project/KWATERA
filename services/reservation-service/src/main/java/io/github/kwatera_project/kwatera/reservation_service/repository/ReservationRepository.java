package io.github.kwatera_project.kwatera.reservation_service.repository;

import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
  List<Reservation> findByUnitId(UUID unitId);
}
