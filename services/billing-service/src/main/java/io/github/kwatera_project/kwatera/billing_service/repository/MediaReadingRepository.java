package io.github.kwatera_project.kwatera.billing_service.repository;

import io.github.kwatera_project.kwatera.billing_service.model.MediaReading;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaReadingRepository extends JpaRepository<MediaReading, UUID> {
  Optional<MediaReading> findBySettlementId(UUID settlementId);
}
