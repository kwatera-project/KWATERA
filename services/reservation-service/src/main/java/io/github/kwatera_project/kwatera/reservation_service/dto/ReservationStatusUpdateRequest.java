package io.github.kwatera_project.kwatera.reservation_service.dto;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;

public class ReservationStatusUpdateRequest {
  private ReservationStatus newStatus;

  public ReservationStatus getNewStatus() {
    return newStatus;
  }

  public void setNewStatus(ReservationStatus newStatus) {
    this.newStatus = newStatus;
  }
}
