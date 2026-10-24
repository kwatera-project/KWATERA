package io.github.kwatera_project.kwatera.reservation_service.conversation.service;

import io.github.kwatera_project.kwatera.reservation_service.conversation.model.Conversation;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.service.BusinessDateProvider;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ConversationLifecyclePolicy {

  private final BusinessDateProvider businessDateProvider;
  private final long retentionDays;

  public ConversationLifecyclePolicy(
      BusinessDateProvider businessDateProvider,
      @Value("${kwatera.conversation.retention-days:90}") long retentionDays) {
    this.businessDateProvider = businessDateProvider;
    this.retentionDays = retentionDays;
  }

  public void requireAvailable(Reservation reservation, Conversation conversation) {
    if (reservation.getStatus() == null) {
      throw conflict("Reservation status is unavailable");
    }

    switch (reservation.getStatus()) {
      case PENDING, CONFIRMED -> {
        return;
      }
      case BLOCKED -> throw conflict("Blocked reservations cannot have conversations");
      case COMPLETED, CANCELLED -> {
        LocalDate expiresOn =
            retentionExpiresOn(reservation, conversation)
                .orElseThrow(
                    () -> conflict("Reservation lifecycle date is unavailable for retention"));
        if (businessDateProvider.today().isAfter(expiresOn)) {
          throw conflict("Conversation retention period has expired");
        }
      }
    }
  }

  public boolean isAvailable(Reservation reservation, Conversation conversation) {
    try {
      requireAvailable(reservation, conversation);
      return true;
    } catch (ResponseStatusException exception) {
      return false;
    }
  }

  public Optional<LocalDate> retentionExpiresOn(
      Reservation reservation, Conversation conversation) {
    LocalDate lifecycleAnchor =
        switch (reservation.getStatus()) {
          case COMPLETED -> reservation.getEndDate();
          case CANCELLED ->
              reservation.getCancelledAt() == null
                  ? null
                  : businessDateProvider.toLocalDate(reservation.getCancelledAt());
          case PENDING, CONFIRMED, BLOCKED -> null;
        };

    if (lifecycleAnchor == null) {
      return Optional.empty();
    }

    if (conversation != null && conversation.getLastMessageAt() != null) {
      LocalDate lastMessageDate = businessDateProvider.toLocalDate(conversation.getLastMessageAt());
      if (lastMessageDate.isAfter(lifecycleAnchor)) {
        lifecycleAnchor = lastMessageDate;
      }
    }
    return Optional.of(lifecycleAnchor.plusDays(retentionDays));
  }

  private ResponseStatusException conflict(String reason) {
    return new ResponseStatusException(HttpStatus.CONFLICT, reason);
  }
}
