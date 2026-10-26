package io.github.kwatera_project.kwatera.reservation_service.conversation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "conversations",
    uniqueConstraints =
        @UniqueConstraint(name = "uq_conversations_reservation", columnNames = "reservation_id"))
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Conversation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "reservation_id", nullable = false, updatable = false)
  private UUID reservationId;

  @Column(name = "guest_user_id", nullable = false, updatable = false)
  private UUID guestUserId;

  @Column(name = "owner_user_id", nullable = false, updatable = false)
  private UUID ownerUserId;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "last_message_at")
  private Instant lastMessageAt;
}
