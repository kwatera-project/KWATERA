package io.github.kwatera_project.kwatera.reservation_service.conversation.websocket;

import io.github.kwatera_project.kwatera.reservation_service.conversation.dto.MessageResponse;
import java.util.UUID;

/** Immutable persisted result and authoritative participant snapshot; no transport credentials. */
public record MessageCreated(MessageResponse message, UUID guestUserId, UUID ownerUserId) {}
