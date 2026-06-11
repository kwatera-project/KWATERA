package io.github.kwatera_project.kwatera.billing_service.dto;

import java.util.UUID;

public record InternalSystemEventRequest(
    String actionType, UUID actorUserId, String entityType, UUID entityId, String details) {}
