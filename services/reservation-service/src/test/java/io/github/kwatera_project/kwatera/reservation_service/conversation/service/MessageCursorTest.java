package io.github.kwatera_project.kwatera.reservation_service.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MessageCursorTest {
  private final UUID conversation = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private final UUID message = UUID.fromString("ffffffff-0000-0000-0000-000000000001");

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2026-10-27T12:00:00Z",
        "2026-10-27T12:00:00.000001Z",
        "2026-10-27T12:00:00.123456Z"
      })
  void cursorRoundTripsExactTimestampAndIds(String timestamp) {
    var cursor = new MessageCursor(conversation, Instant.parse(timestamp), message);
    assertThat(MessageCursor.decode(cursor.encode(), conversation)).isEqualTo(cursor);
  }

  @Test
  void cursorCannotBeReusedForAnotherConversation() {
    var cursor = new MessageCursor(conversation, Instant.parse("2026-10-27T12:00:00Z"), message);
    assertBadCursor(cursor.encode(), UUID.randomUUID());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "0|22222222-2222-2222-2222-222222222222|2026-10-27T12:00:00Z|ffffffff-0000-0000-0000-000000000001",
        "1|22222222-2222-2222-2222-222222222222|2026-10-27T12:00:00.123456789Z|ffffffff-0000-0000-0000-000000000001",
        "1|22222222-2222-2222-2222-222222222222|bad|ffffffff-0000-0000-0000-000000000001",
        "1|22222222-2222-2222-2222-222222222222|+10000-01-01T00:00:00Z|ffffffff-0000-0000-0000-000000000001",
        "1|22222222-2222-2222-2222-222222222222|2026-10-27T12:00:00Z|f-0-0-0-1",
        "1|22222222-2222-2222-2222-222222222222|2026-10-27T12:00:00Z|ffffffff-0000-0000-0000-000000000001|extra"
      })
  void rejectsInvalidPayloadsWithoutPassingInvalidValuesToDatabase(String payload) {
    assertBadCursor(
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8)),
        conversation);
  }

  @Test
  void rejectsOversizedPayloadBeforeDecoding() {
    assertBadCursor("a".repeat(257), conversation);
  }

  private void assertBadCursor(String encoded, UUID scope) {
    assertThatThrownBy(() -> MessageCursor.decode(encoded, scope))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }
}
