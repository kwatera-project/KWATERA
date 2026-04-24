package io.github.kwatera_project.kwatera.reservation_service.dto;

public class AvailabilityDto {

  private boolean available;
  private String message;

  public AvailabilityDto(boolean available, String message) {
    this.available = available;
    this.message = message;
  }

  public boolean isAvailable() {
    return available;
  }

  public String getMessage() {
    return message;
  }
}
