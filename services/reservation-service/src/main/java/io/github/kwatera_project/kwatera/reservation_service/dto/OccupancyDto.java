package io.github.kwatera_project.kwatera.reservation_service.dto;

import java.time.LocalDate;
import java.util.UUID;

public class OccupancyDto {

  private UUID reservationId;
  private UUID unitId;
  private LocalDate startDate;
  private LocalDate endDate;
  private String status;

  public OccupancyDto() {}

  public OccupancyDto(
      UUID reservationId, UUID unitId, LocalDate startDate, LocalDate endDate, String status) {
    this.reservationId = reservationId;
    this.unitId = unitId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.status = status;
  }

  public UUID getReservationId() {
    return reservationId;
  }

  public void setReservationId(UUID reservationId) {
    this.reservationId = reservationId;
  }

  public UUID getUnitId() {
    return unitId;
  }

  public void setUnitId(UUID unitId) {
    this.unitId = unitId;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
