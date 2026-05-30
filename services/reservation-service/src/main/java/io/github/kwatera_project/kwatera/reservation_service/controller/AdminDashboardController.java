package io.github.kwatera_project.kwatera.reservation_service.controller;

import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

  private final ReservationService reservationService;

  @GetMapping("/reservations")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
  public ReservationMetricsDto getReservationMetrics(
      @RequestParam(name = "startDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(name = "endDate", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      Authentication authentication) {

    var authCtx =
        io.github.kwatera_project.kwatera.reservation_service.utils.AuthenticationUtil
            .getAuthContext(authentication);
    return reservationService.getDashboardReservationMetrics(
        startDate, endDate, authCtx.userId(), authCtx.isAdmin());
  }
}
