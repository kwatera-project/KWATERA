package io.github.kwatera_project.kwatera.reservation_service.conversation.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conversation_messages")
@Getter
@NoArgsConstructor
public class Message {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
  private Conversation conversation;

  @Column(name = "sender_user_id", nullable = false, updatable = false)
  private UUID senderUserId;

  @Column(nullable = false, updatable = false, length = 4000)
  private String content;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification =
          "JPA association must retain the managed conversation, not a detached copy; entities never leave the service boundary")
  public Message(Conversation conversation, UUID senderUserId, String content) {
    this.conversation = conversation;
    this.senderUserId = senderUserId;
    this.content = content;
  }

  @PrePersist
  void assignCreationTime() {
    // PostgreSQL stores microseconds: the returned cursor must use the stored precision.
    createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
