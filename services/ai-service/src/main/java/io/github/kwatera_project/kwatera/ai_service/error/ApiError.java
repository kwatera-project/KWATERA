package io.github.kwatera_project.kwatera.ai_service.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Controlled API error; omits backend bodies, input contents and stack traces")
public record ApiError(
    @Schema(
            description = "Stable machine-readable error code",
            example = "AI_UNAVAILABLE",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
    @Schema(
            description = "Safe error description",
            example = "The AI backend is temporarily unavailable.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String message,
    @Schema(
            description = "HTTP response status",
            example = "503",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int status,
    @Schema(
            description = "UTC error timestamp",
            example = "2026-11-05T10:00:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Instant timestamp) {
  public static ApiError of(String code, String message, int status) {
    return new ApiError(code, message, status, Instant.now());
  }
}
