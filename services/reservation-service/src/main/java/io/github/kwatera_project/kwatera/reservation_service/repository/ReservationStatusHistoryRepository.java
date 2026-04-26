package io.github.kwatera_project.kwatera.reservation_service.repository;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationStatusHistoryRepository
    extends JpaRepository<ReservationStatusHistory, UUID> {}
