package io.github.kwatera_project.kwatera.reservation_service.controller;

import io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/admin/occupancy")
@RequiredArgsConstructor
public class AdminOccupancyController {

  private final ReservationService reservationService;

  @GetMapping
  public List<OccupancyDto> getOccupancy(
      @RequestParam("startDate") LocalDate startDate,
      @RequestParam("endDate") LocalDate endDate,
      Authentication authentication) {
    var authCtx =
        io.github.kwatera_project.kwatera.reservation_service.utils.AuthenticationUtil
            .getAuthContext(authentication);
    return reservationService.getOccupancy(startDate, endDate, authCtx.userId(), authCtx.isAdmin());
  }
}
