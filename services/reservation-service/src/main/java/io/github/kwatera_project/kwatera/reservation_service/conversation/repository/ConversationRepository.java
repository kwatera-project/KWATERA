package io.github.kwatera_project.kwatera.reservation_service.conversation.repository;

import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Conversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  Optional<Conversation> findByReservationId(UUID reservationId);

  @Query(
      "SELECT c FROM Conversation c WHERE c.id = :conversationId "
          + "AND (c.guestUserId = :userId OR c.ownerUserId = :userId)")
  Optional<Conversation> findAccessibleById(
      @Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

  @Query(
      "SELECT DISTINCT c FROM Conversation c "
          + "WHERE c.guestUserId = :userId OR c.ownerUserId = :userId "
          + "ORDER BY c.createdAt DESC")
  List<Conversation> findAllAccessibleByUserId(@Param("userId") UUID userId);
}
