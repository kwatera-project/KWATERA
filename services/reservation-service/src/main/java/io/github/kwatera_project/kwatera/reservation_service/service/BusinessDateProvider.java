package io.github.kwatera_project.kwatera.reservation_service.service;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BusinessDateProvider {

  private final ZoneId businessZone;

  public BusinessDateProvider(
      @Value("${kwatera.business-zone:Europe/Warsaw}") String businessZone) {
    this.businessZone = ZoneId.of(businessZone);
  }

  public LocalDate today() {
    return LocalDate.now(businessZone);
  }
}
