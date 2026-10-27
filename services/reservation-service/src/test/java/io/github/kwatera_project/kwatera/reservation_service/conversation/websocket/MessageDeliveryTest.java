package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.MessageResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class MessageDeliveryTest {
  @Test
  void identicalRecipientsReceiveOneDelivery() {
    var messaging = mock(SimpMessagingTemplate.class);
    UUID user = UUID.randomUUID();
    var message =
        new MessageResponse(
            UUID.randomUUID(), UUID.randomUUID(), user, "private content", Instant.now());
    new MessageDelivery(messaging).afterCommit(new MessageCreated(message, user, user));
    verify(messaging)
        .convertAndSendToUser(
            user.toString(),
            "/queue/conversations/" + message.conversationId() + "/messages",
            message);
    verifyNoMoreInteractions(messaging);
  }

  @Test
  void failureAfterCommitDoesNotFailTheWriteOrPreventOtherRecipientDelivery() {
    var messaging = mock(SimpMessagingTemplate.class);
    UUID guest = UUID.randomUUID(), owner = UUID.randomUUID();
    var message =
        new MessageResponse(
            UUID.randomUUID(), UUID.randomUUID(), guest, "private content", Instant.now());
    String destination = "/queue/conversations/" + message.conversationId() + "/messages";
    doThrow(new IllegalStateException("broker unavailable"))
        .when(messaging)
        .convertAndSendToUser(guest.toString(), destination, message);
    new MessageDelivery(messaging).afterCommit(new MessageCreated(message, guest, owner));
    verify(messaging).convertAndSendToUser(owner.toString(), destination, message);
  }
}
