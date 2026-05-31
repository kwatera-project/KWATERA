package io.github.kwatera_project.kwatera.ai_pricing_service.exception;

import java.util.UUID;

public class PropertyNotFoundException extends RuntimeException {
  public PropertyNotFoundException(UUID id) {
    super("Property not found: " + id);
  }
}
