package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import org.springframework.messaging.MessagingException;

final class MessagingFailure extends MessagingException {
  private static final long serialVersionUID = 1L;
  private final String code;

  MessagingFailure(String code) {
    super("Messaging request rejected");
    this.code = code;
  }

  MessagingError error() {
    return new MessagingError(code, "Messaging request rejected; reconnect if disconnected");
  }
}
