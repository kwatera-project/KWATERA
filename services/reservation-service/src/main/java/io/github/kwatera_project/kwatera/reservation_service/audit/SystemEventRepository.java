package io.github.kwatera_project.kwatera.reservation_service.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemEventRepository extends JpaRepository<SystemEvent, UUID> {

  Page<SystemEvent> findByActionType(SystemEventType actionType, Pageable pageable);
}
