package io.github.kwatera_project.kwatera.billing_service.repository;

import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
  Optional<Settlement> findByReservationId(UUID reservationId);

  @Query(
      "SELECT COALESCE(SUM(s.amountPaid), 0) FROM Settlement s "
          + "WHERE s.reservationId IN :reservationIds "
          + "AND s.status <> io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.CANCELLED "
          + "AND s.createdAt >= :start AND s.createdAt <= :end")
  BigDecimal sumRevenueForReservationsInPeriod(
      @Param("reservationIds") List<UUID> reservationIds,
      @Param("start") Instant start,
      @Param("end") Instant end);

  @Query(
      "SELECT COALESCE(SUM(s.balanceDue), 0) FROM Settlement s "
          + "WHERE s.reservationId IN :reservationIds "
          + "AND s.status <> io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.CANCELLED "
          + "AND s.createdAt >= :start AND s.createdAt <= :end")
  BigDecimal sumUnpaidBalanceForReservationsInPeriod(
      @Param("reservationIds") List<UUID> reservationIds,
      @Param("start") Instant start,
      @Param("end") Instant end);

  @Query(
      "SELECT COUNT(s) FROM Settlement s "
          + "WHERE s.reservationId IN :reservationIds "
          + "AND s.status = :status "
          + "AND s.createdAt >= :start AND s.createdAt <= :end")
  long countByStatusForReservationsInPeriod(
      @Param("reservationIds") List<UUID> reservationIds,
      @Param("status") SettlementStatus status,
      @Param("start") Instant start,
      @Param("end") Instant end);

  @Query(
      "SELECT COUNT(s) FROM Settlement s "
          + "WHERE s.reservationId IN :reservationIds "
          + "AND s.status IN (io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.ISSUED, io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.PARTIALLY_PAID, io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus.DRAFT) "
          + "AND s.createdAt >= :start AND s.createdAt <= :end")
  long countUnpaidSettlementsForReservationsInPeriod(
      @Param("reservationIds") List<UUID> reservationIds,
      @Param("start") Instant start,
      @Param("end") Instant end);
}
