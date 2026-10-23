package io.github.kwatera_project.kwatera.reservation_service.conversation.service;

import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Conversation;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ConversationParticipantAuthorizer {

  private static final String NOT_FOUND = "Conversation or reservation not found";

  public void requireConversationParticipant(Conversation conversation, UUID currentUserId) {
    if (!currentUserId.equals(conversation.getGuestUserId())
        && !currentUserId.equals(conversation.getOwnerUserId())) {
      throw notFound();
    }
  }

  public void requireReservationParticipant(
      Reservation reservation, UUID ownerId, UUID currentUserId) {
    boolean isGuest =
        reservation.getStatus() != ReservationStatus.BLOCKED
            && currentUserId.equals(reservation.getUserId());
    if (!isGuest && !currentUserId.equals(ownerId)) {
      throw notFound();
    }
  }

  private ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND);
  }
}
