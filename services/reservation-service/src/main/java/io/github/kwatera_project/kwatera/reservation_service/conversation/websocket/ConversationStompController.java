package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import io.github.kwatera_project.kwatera.reservation_service.conversation.service.ConversationAuthenticationService;
import io.github.kwatera_project.kwatera.reservation_service.conversation.service.MessageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ConversationStompController {
  private final MessageService messages;
  private final ConversationAuthenticationService authentication;

  @MessageMapping("/conversations/{conversationId}/messages")
  public void send(
      @DestinationVariable("conversationId") UUID conversationId,
      @Payload tools.jackson.databind.JsonNode body,
      Authentication principal) {
    var user = authentication.requireUserId(principal);
    if (!body.isObject()
        || body.size() != 1
        || !body.has("content")
        || !body.get("content").isString()) {
      throw new MessagingFailure("INVALID_MESSAGE");
    }
    messages.send(conversationId, user, body.get("content").asString());
  }

  @MessageExceptionHandler
  @SendToUser(value = "/queue/messaging-errors", broadcast = false)
  public MessagingError error(Exception failure) {
    return MessagingError.from(failure);
  }
}
