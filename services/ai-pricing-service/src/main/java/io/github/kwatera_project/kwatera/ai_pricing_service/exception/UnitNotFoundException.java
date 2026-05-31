package io.github.kwatera_project.kwatera.ai_pricing_service.exception;

import java.util.UUID;

public class UnitNotFoundException extends RuntimeException {
  public UnitNotFoundException(UUID id) {
    super("Unit not found: " + id);
  }
}
