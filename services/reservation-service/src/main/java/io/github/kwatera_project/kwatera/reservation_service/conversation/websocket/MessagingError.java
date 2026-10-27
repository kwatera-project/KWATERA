package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import org.springframework.web.server.ResponseStatusException;

public record MessagingError(String code, String message) {
  public static MessagingError from(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof MessagingFailure rejected) return rejected.error();
      if (cause instanceof ResponseStatusException status) {
        return switch (status.getStatusCode().value()) {
          case 401 ->
              new MessagingError("UNAUTHENTICATED", "Valid credentials required; reconnect");
          case 404 ->
              new MessagingError("CONVERSATION_NOT_FOUND", "Conversation or reservation not found");
          case 409 -> new MessagingError("CONVERSATION_UNAVAILABLE", "Conversation unavailable");
          default -> new MessagingError("INVALID_MESSAGE", "Message content is invalid");
        };
      }
    }
    return new MessagingError("INVALID_MESSAGE", "Message could not be processed");
  }
}
