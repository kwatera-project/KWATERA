package io.github.kwatera_project.kwatera.reservation_service.conversation.repository;

import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Message;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {
  @Query(
      "SELECT m FROM Message m WHERE m.conversation.id = :conversationId "
          + "ORDER BY m.createdAt DESC, m.id DESC")
  List<Message> findNewest(@Param("conversationId") UUID conversationId, Limit limit);

  @Query(
      "SELECT m FROM Message m WHERE m.conversation.id = :conversationId "
          + "AND (m.createdAt < :createdAt OR (m.createdAt = :createdAt AND m.id < :id)) "
          + "ORDER BY m.createdAt DESC, m.id DESC")
  List<Message> findOlder(
      @Param("conversationId") UUID conversationId,
      @Param("createdAt") Instant createdAt,
      @Param("id") UUID id,
      Limit limit);
}
