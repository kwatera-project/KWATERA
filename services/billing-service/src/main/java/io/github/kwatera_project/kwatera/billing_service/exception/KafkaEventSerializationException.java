package io.github.kwatera_project.kwatera.billing_service.exception;

public class KafkaEventSerializationException extends RuntimeException {
  public KafkaEventSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
