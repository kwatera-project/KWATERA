package io.github.kwatera_project.kwatera.reservation_service.dto;

import java.time.LocalDate;
import java.util.UUID;

public class OccupancyDto {

  private UUID reservationId;
  private UUID unitId;
  private String unitName;
  private LocalDate startDate;
  private LocalDate endDate;
  private String status;
  private String guestEmail;
  private String guestName;

  public OccupancyDto() {}

  public OccupancyDto(
      UUID reservationId,
      UUID unitId,
      String unitName,
      LocalDate startDate,
      LocalDate endDate,
      String status,
      String guestEmail,
      String guestName) {
    this.reservationId = reservationId;
    this.unitId = unitId;
    this.unitName = unitName;
    this.startDate = startDate;
    this.endDate = endDate;
    this.status = status;
    this.guestEmail = guestEmail;
    this.guestName = guestName;
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

  public String getUnitName() {
    return unitName;
  }

  public void setUnitName(String unitName) {
    this.unitName = unitName;
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

  public String getGuestEmail() {
    return guestEmail;
  }

  public void setGuestEmail(String guestEmail) {
    this.guestEmail = guestEmail;
  }

  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }
}
