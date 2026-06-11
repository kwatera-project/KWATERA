package io.github.kwatera_project.kwatera.reservation_service.audit;

import java.time.Instant;
import java.util.UUID;

public record SystemEventResponseDto(
    UUID id,
    Instant timestamp,
    SystemEventType actionType,
    UUID actorUserId,
    String entityType,
    UUID entityId,
    String details) {}
