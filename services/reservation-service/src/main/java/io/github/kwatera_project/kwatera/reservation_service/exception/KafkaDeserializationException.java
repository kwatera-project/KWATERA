package io.github.kwatera_project.kwatera.reservation_service.exception;

public class KafkaDeserializationException extends RuntimeException {
  public KafkaDeserializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
