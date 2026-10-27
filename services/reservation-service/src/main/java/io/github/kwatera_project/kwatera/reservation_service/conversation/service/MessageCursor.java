package io.github.kwatera_project.kwatera.reservation_service.conversation.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

record MessageCursor(UUID conversationId, Instant createdAt, UUID id) {
  String encode() {
    String payload = "1|" + conversationId + "|" + createdAt + "|" + id;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
  }

  static MessageCursor decode(String encoded, UUID conversationId) {
    try {
      if (encoded.length() > 256 || !encoded.matches("[A-Za-z0-9_-]+")) {
        throw new IllegalArgumentException();
      }
      String[] parts =
          new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
              .split("\\|", -1);
      if (parts.length != 4 || !parts[0].equals("1")) {
        throw new IllegalArgumentException();
      }
      MessageCursor cursor =
          new MessageCursor(
              UUID.fromString(parts[1]), Instant.parse(parts[2]), UUID.fromString(parts[3]));
      if (!cursor.conversationId.equals(conversationId)
          || !cursor.encode().equals(encoded)
          || cursor.createdAt.getNano() % 1000 != 0
          || cursor.createdAt.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
          || cursor.createdAt.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z"))) {
        throw new IllegalArgumentException();
      }
      return cursor;
    } catch (IllegalArgumentException | java.time.DateTimeException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid message cursor", exception);
    }
  }
}
