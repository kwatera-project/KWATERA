package io.github.kwatera_project.kwatera.billing_service.model;

public enum ReadingStatus {
  PENDING,
  AUTO_APPROVED,
  REQUEST_REUPLOAD,
  REQUEST_MANUAL_REVIEW,
  MANUALLY_APPROVED
}
