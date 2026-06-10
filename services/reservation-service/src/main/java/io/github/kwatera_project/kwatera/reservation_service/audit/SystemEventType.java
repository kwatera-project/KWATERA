package io.github.kwatera_project.kwatera.reservation_service.audit;

public enum SystemEventType {
  RESERVATION_CREATED,
  MANUAL_RESERVATION_CREATED,
  UNIT_BLOCKED,
  RESERVATION_STATUS_CHANGED,
  EXPIRED_RESERVATION_CANCELLED
}
