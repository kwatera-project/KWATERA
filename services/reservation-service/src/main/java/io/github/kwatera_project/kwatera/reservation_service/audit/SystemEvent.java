package io.github.kwatera_project.kwatera.reservation_service.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_events")
@Getter
@Setter
@NoArgsConstructor
public class SystemEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private Instant timestamp;

  @Column(name = "action_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private SystemEventType actionType;

  @Column(name = "actor_user_id")
  private UUID actorUserId;

  @Column(name = "entity_type")
  private String entityType;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(columnDefinition = "TEXT")
  private String details;
}
