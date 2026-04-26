package io.github.kwatera_project.kwatera.reservation_service.controller;

import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationOverviewDto;
import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationStatusUpdateRequest;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.service.AdminReservationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
public class AdminReservationController {

  private final AdminReservationService adminReservationService;

  @GetMapping
  public List<ReservationOverviewDto> getReservations(
      @RequestParam(name = "status", required = false) ReservationStatus status,
      Authentication authentication) {

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Token is missing");
    }

    Object details = authentication.getDetails();
    if (!(details instanceof String userIdString) || userIdString.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }

    UUID ownerId;
    try {
      ownerId = UUID.fromString(userIdString);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    return adminReservationService.getReservationsOverview(ownerId, status, isAdmin);
  }

  @PatchMapping("/{reservationId}/status")
  public ReservationOverviewDto updateReservationStatus(
      @PathVariable(name = "reservationId") UUID reservationId,
      @RequestBody ReservationStatusUpdateRequest request,
      Authentication authentication) {

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Token is missing");
    }

    Object details = authentication.getDetails();
    if (!(details instanceof String userIdString) || userIdString.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }

    UUID userId;
    try {
      userId = UUID.fromString(userIdString);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }
    boolean isAdmin =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    return adminReservationService.updateReservationStatus(
        reservationId, request.getNewStatus(), userId, isAdmin);
  }
}
