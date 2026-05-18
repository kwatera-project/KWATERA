package io.github.kwatera_project.kwatera.billing_service.repository;

import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
  Optional<Settlement> findByReservationId(UUID reservationId);
}
