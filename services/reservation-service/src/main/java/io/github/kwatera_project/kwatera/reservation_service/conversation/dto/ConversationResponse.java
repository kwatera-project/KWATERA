package io.github.kwatera_project.kwatera.reservation_service.conversation.dto;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConversationResponse(
    UUID id,
    UUID reservationId,
    UUID guestUserId,
    UUID ownerUserId,
    ReservationStatus reservationStatus,
    Instant createdAt,
    Instant lastMessageAt,
    LocalDate retentionExpiresOn) {}
