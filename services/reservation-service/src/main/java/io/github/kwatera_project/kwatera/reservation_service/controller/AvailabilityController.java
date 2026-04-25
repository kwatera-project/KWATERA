package io.github.kwatera_project.kwatera.reservation_service.controller;

import io.github.kwatera_project.kwatera.reservation_service.dto.AvailabilityDto;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/availability")
@CrossOrigin(origins = {"http://localhost:5173"})
public class AvailabilityController {

  private final ReservationService reservationService;

  public AvailabilityController(ReservationService reservationService) {

    this.reservationService = reservationService;
  }

  @GetMapping
  public AvailabilityDto checkAvailability(
      @RequestParam("unitId") UUID unitId,
      @RequestParam("from") LocalDate from,
      @RequestParam("to") LocalDate to) {
    return reservationService.checkAvailability(unitId, from, to);
  }
}
