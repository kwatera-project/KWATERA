package io.github.kwatera_project.kwatera.reservation_service.conversation.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Conversation;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ConversationParticipantAuthorizerTest {

  private final ConversationParticipantAuthorizer authorizer =
      new ConversationParticipantAuthorizer();

  @Test
  void shouldAllowOnlySnapshottedConversationParticipants() {
    UUID guestId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    Conversation conversation = new Conversation();
    conversation.setGuestUserId(guestId);
    conversation.setOwnerUserId(ownerId);

    assertThatCode(() -> authorizer.requireConversationParticipant(conversation, guestId))
        .doesNotThrowAnyException();
    assertThatCode(() -> authorizer.requireConversationParticipant(conversation, ownerId))
        .doesNotThrowAnyException();
    assertThatThrownBy(
            () -> authorizer.requireConversationParticipant(conversation, UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404 NOT_FOUND");
  }

  @Test
  void blockedReservationActorShouldNotBeTreatedAsGuest() {
    UUID actorId = UUID.randomUUID();
    Reservation reservation = new Reservation();
    reservation.setUserId(actorId);
    reservation.setStatus(ReservationStatus.BLOCKED);

    assertThatThrownBy(
            () -> authorizer.requireReservationParticipant(reservation, UUID.randomUUID(), actorId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404 NOT_FOUND");
  }
}
