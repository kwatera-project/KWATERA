package io.github.kwatera_project.kwatera.billing_service.exception;

public class WebhookProcessingException extends RuntimeException {
  public WebhookProcessingException(String message) {
    super(message);
  }

  public WebhookProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
