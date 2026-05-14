package io.github.kwatera_project.kwatera.reservation_service.controller;

import io.github.kwatera_project.kwatera.reservation_service.dto.CreateReservationRequest;
import io.github.kwatera_project.kwatera.reservation_service.dto.GuestReservationDto;
import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationDetailsDto;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Reservation createReservation(
      @jakarta.validation.Valid @RequestBody CreateReservationRequest request,
      Authentication authentication) {
    UUID guestId = validateAndGetUserId(authentication);
    return reservationService.createReservation(guestId, request);
  }

  @GetMapping("/my")
  public List<GuestReservationDto> getMyReservations(Authentication authentication) {
    UUID userId = validateAndGetUserId(authentication);

    boolean isGuest =
        authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_GUEST"));
    if (!isGuest) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    return reservationService.getMyReservations(userId);
  }

  @GetMapping("/{reservationId}")
  public ReservationDetailsDto getReservationDetails(
      @PathVariable("reservationId") UUID reservationId, Authentication authentication) {
    UUID userId = validateAndGetUserId(authentication);

    boolean isAdmin =
        authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

    boolean isOwner =
        authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OWNER"));

    return reservationService.getReservationDetails(reservationId, userId, isAdmin, isOwner);
  }

  private UUID validateAndGetUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Token is missing");
    }
    Object details = authentication.getDetails();
    if (!(details instanceof String) || ((String) details).trim().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }
    try {
      return UUID.fromString((String) details);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid token format");
    }
  }
}
