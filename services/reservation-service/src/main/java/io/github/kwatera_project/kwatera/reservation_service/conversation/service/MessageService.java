package io.github.kwatera_project.kwatera.reservation_service.conversation.service;

import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.MessageHistoryResponse;
import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.MessageResponse;
import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Conversation;
import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Message;
import io.github.kwatera_project.kwatera.reservation_service.conversation.repository.ConversationRepository;
import io.github.kwatera_project.kwatera.reservation_service.conversation.repository.MessageRepository;
import io.github.kwatera_project.kwatera.reservation_service.conversation.websocket.MessageCreated;
import io.github.kwatera_project.kwatera.reservation_service.repository.ReservationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Application boundary. Adapters supply the trusted authenticated user, never a request sender. */
@Service
@RequiredArgsConstructor
public class MessageService {
  private final ConversationRepository conversations;
  private final ReservationRepository reservations;
  private final MessageRepository messages;
  private final ConversationParticipantAuthorizer authorizer;
  private final ConversationLifecyclePolicy lifecycle;
  private final ApplicationEventPublisher events;

  @Transactional
  public MessageResponse send(UUID conversationId, UUID currentUserId, String content) {
    Conversation conversation = requireAccess(conversationId, currentUserId, true);
    String normalized = content == null ? null : content.strip();
    if (normalized == null || normalized.isEmpty() || normalized.length() > 4000) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Content must contain 1 to 4000 characters after stripping whitespace");
    }
    Message message = messages.saveAndFlush(new Message(conversation, currentUserId, normalized));
    if (conversation.getLastMessageAt() == null
        || message.getCreatedAt().isAfter(conversation.getLastMessageAt())) {
      conversation.setLastMessageAt(message.getCreatedAt());
    }
    MessageResponse result = response(message);
    events.publishEvent(
        new MessageCreated(result, conversation.getGuestUserId(), conversation.getOwnerUserId()));
    return result;
  }

  @Transactional(readOnly = true)
  public MessageHistoryResponse history(
      UUID conversationId, UUID currentUserId, String limit, String cursor) {
    requireAccess(conversationId, currentUserId, false);
    int size = parseLimit(limit);
    MessageCursor boundary = cursor == null ? null : MessageCursor.decode(cursor, conversationId);
    // Fetch one extra row; only the immutable tuple boundary advances traversal.
    List<Message> found =
        boundary == null
            ? messages.findNewest(conversationId, Limit.of(size + 1))
            : messages.findOlder(
                conversationId, boundary.createdAt(), boundary.id(), Limit.of(size + 1));
    boolean hasMore = found.size() > size;
    List<MessageResponse> items = found.stream().limit(size).map(this::response).toList();
    String nextCursor = null;
    if (hasMore) {
      MessageResponse last = items.getLast();
      nextCursor = new MessageCursor(conversationId, last.createdAt(), last.id()).encode();
    }
    return new MessageHistoryResponse(items, nextCursor, hasMore);
  }

  private Conversation requireAccess(UUID id, UUID userId, boolean forUpdate) {
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
    Conversation conversation =
        (forUpdate
                ? conversations.findAccessibleForUpdate(id, userId)
                : conversations.findAccessibleById(id, userId))
            .orElseThrow(this::notFound);
    authorizer.requireConversationParticipant(conversation, userId);
    var reservation =
        reservations.findById(conversation.getReservationId()).orElseThrow(this::notFound);
    lifecycle.requireAvailable(reservation, conversation);
    return conversation;
  }

  private int parseLimit(String limit) {
    try {
      int value = limit == null ? 50 : Integer.parseInt(limit);
      if (value < 1 || value > 100) {
        throw new NumberFormatException();
      }
      return value;
    } catch (NumberFormatException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Limit must be an integer from 1 to 100", exception);
    }
  }

  private MessageResponse response(Message message) {
    return new MessageResponse(
        message.getId(),
        message.getConversation().getId(),
        message.getSenderUserId(),
        message.getContent(),
        message.getCreatedAt());
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(
        HttpStatus.NOT_FOUND, "Conversation or reservation not found");
  }
}
