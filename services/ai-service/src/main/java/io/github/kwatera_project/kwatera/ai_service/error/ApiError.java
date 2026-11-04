package io.github.kwatera_project.kwatera.ai_service.error;

import java.time.Instant;

public record ApiError(String code, String message, int status, Instant timestamp) {
  public static ApiError of(String code, String message, int status) {
    return new ApiError(code, message, status, Instant.now());
  }
}
