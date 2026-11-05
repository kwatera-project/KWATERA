package io.github.kwatera_project.kwatera.ai_service.error;

import org.springframework.http.HttpStatus;

public class AiException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public AiException(String code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String code() {
    return code;
  }

  public HttpStatus status() {
    return status;
  }

  public static AiException invalidResponse() {
    return new AiException(
        "AI_INVALID_RESPONSE",
        HttpStatus.BAD_GATEWAY,
        "The AI backend returned an invalid or incomplete response.");
  }
}
