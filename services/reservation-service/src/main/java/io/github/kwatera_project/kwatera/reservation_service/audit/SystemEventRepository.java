package io.github.kwatera_project.kwatera.reservation_service.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemEventRepository extends JpaRepository<SystemEvent, UUID> {

  Page<SystemEvent> findByActionType(SystemEventType actionType, Pageable pageable);

  Page<SystemEvent> findByTimestampGreaterThanEqual(Instant from, Pageable pageable);

  Page<SystemEvent> findByTimestampLessThanEqual(Instant to, Pageable pageable);

  Page<SystemEvent> findByTimestampBetween(Instant from, Instant to, Pageable pageable);

  Page<SystemEvent> findByActionTypeAndTimestampGreaterThanEqual(
      SystemEventType actionType, Instant from, Pageable pageable);

  Page<SystemEvent> findByActionTypeAndTimestampLessThanEqual(
      SystemEventType actionType, Instant to, Pageable pageable);

  Page<SystemEvent> findByActionTypeAndTimestampBetween(
      SystemEventType actionType, Instant from, Instant to, Pageable pageable);
}
