package io.github.kwatera_project.kwatera.reservation_service.repository;

import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

  List<Reservation> findByUnitIdIn(List<UUID> unitIds);

  List<Reservation> findByUnitIdInAndStatus(List<UUID> unitIds, ReservationStatus status);

  List<Reservation> findByStatus(ReservationStatus status);

  List<Reservation> findByUnitId(UUID unitId);

  List<Reservation> findByUserId(UUID userId);

  @Query(
      "SELECT r FROM Reservation r WHERE r.status <> io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus.CANCELLED "
          + "AND r.startDate < :endDate AND r.endDate > :startDate")
  List<Reservation> findActiveReservationsInDateRange(
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  @Query(
      "SELECT r FROM Reservation r WHERE r.unitId IN :unitIds "
          + "AND r.status <> io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus.CANCELLED "
          + "AND r.startDate < :endDate AND r.endDate > :startDate")
  List<Reservation> findActiveReservationsInDateRangeForUnits(
      @Param("unitIds") List<UUID> unitIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      "SELECT COUNT(r) FROM Reservation r WHERE r.status <> io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus.CANCELLED "
          + "AND r.startDate < :endDate AND r.endDate > :startDate")
  long countReservationsInDateRange(
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  @Query(
      "SELECT COUNT(r) FROM Reservation r WHERE r.unitId IN :unitIds "
          + "AND r.status <> io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus.CANCELLED "
          + "AND r.startDate < :endDate AND r.endDate > :startDate")
  long countReservationsInDateRangeForUnits(
      @Param("unitIds") List<UUID> unitIds,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
