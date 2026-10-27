package io.github.kwatera_project.kwatera.reservation_service.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(requiredProperties = {"id", "conversationId", "senderUserId", "content", "createdAt"})
public record MessageResponse(
    UUID id,
    UUID conversationId,
    UUID senderUserId,
    @Schema(example = "Hello, what time can we check in?", maxLength = 4000) String content,
    @Schema(example = "2026-10-27T12:00:00.123456Z") Instant createdAt) {}
