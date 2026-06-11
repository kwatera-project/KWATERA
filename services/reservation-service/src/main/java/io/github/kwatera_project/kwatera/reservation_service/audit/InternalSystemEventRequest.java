package io.github.kwatera_project.kwatera.reservation_service.audit;

import java.util.UUID;

public record InternalSystemEventRequest(
    SystemEventType actionType,
    UUID actorUserId,
    String entityType,
    UUID entityId,
    String details) {}
