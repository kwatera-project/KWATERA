package io.github.kwatera_project.kwatera.reservation_service.controller;

import io.github.kwatera_project.kwatera.reservation_service.dto.OccupancyDto;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    boolean isOwner =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));

    if (!isAdmin && !isOwner) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    Object details = authentication.getDetails();
    UUID ownerId = null;
    if (details instanceof String userIdString && !userIdString.isBlank()) {
      try {
        ownerId = UUID.fromString(userIdString);
      } catch (IllegalArgumentException _) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
      }
    } else {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }

    return reservationService.getOccupancy(startDate, endDate, ownerId, isAdmin);
  }
}
