package io.github.kwatera_project.kwatera.reservation_service.repository;

import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByUnitIdIn(List<UUID> unitIds);
    List<Reservation> findByUnitIdInAndStatus(List<UUID> unitIds, ReservationStatus status);
    List<Reservation> findByStatus(ReservationStatus status);
}