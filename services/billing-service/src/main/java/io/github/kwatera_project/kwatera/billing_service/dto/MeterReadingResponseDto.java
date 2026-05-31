package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.ReadingStatus;

public record MeterReadingResponseDto(ReadingStatus status, String message) {
  public static MeterReadingResponseDto from(ReadingStatus status) {
    String message =
        switch (status) {
          case AUTO_APPROVED -> "Image accepted, reading has been saved.";
          case REQUEST_REUPLOAD -> "Image is unclear,  please upload a clearer photo.";
          case REQUEST_MANUAL_REVIEW -> "Reading sent for manual review by owner.";
          default -> "Reading status: " + status;
        };
    return new MeterReadingResponseDto(status, message);
  }
}
