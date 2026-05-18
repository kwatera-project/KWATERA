package io.github.kwatera_project.kwatera.reservation_service.dto;

import java.time.LocalDate;

public class DateRangeDto {

  private LocalDate startDate;
  private LocalDate endDate;

  public DateRangeDto() {}

  public DateRangeDto(LocalDate startDate, LocalDate endDate) {
    this.startDate = startDate;
    this.endDate = endDate;
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
}
