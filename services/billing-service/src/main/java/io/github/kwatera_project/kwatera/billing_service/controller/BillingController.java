package io.github.kwatera_project.kwatera.billing_service.controller;

import com.stripe.exception.StripeException;
import io.github.kwatera_project.kwatera.billing_service.dto.CheckoutRequest;
import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementItemDto;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementResponseDto;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.service.PaymentService;
import io.github.kwatera_project.kwatera.billing_service.service.StripeService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

  private final PaymentService paymentService;
  private final StripeService stripeService;

  @Value("${jwt.secret}")
  private String secret;

  private static final String AUTHORIZATION_HEADER = "Authorization";

  @PostMapping("/checkout/{reservationId}")
  public ResponseEntity<String> createCheckout(
      @PathVariable("reservationId") UUID reservationId,
      HttpServletRequest request,
      @RequestBody CheckoutRequest checkoutRequest)
      throws StripeException {

    String token = request.getHeader(AUTHORIZATION_HEADER);

    ReservationDto reservation = stripeService.getReservation(reservationId, token);
    authorizeAccess(token, reservation);

    String checkoutUrl =
        paymentService.createCheckoutSession(
            reservationId,
            token,
            checkoutRequest.getType(),
            checkoutRequest.getDescription(),
            checkoutRequest.getQuantity(),
            checkoutRequest.getUnitPrice());

    return ResponseEntity.ok(checkoutUrl);
  }

  @GetMapping("settlements/{reservationId}")
  public ResponseEntity<SettlementResponseDto> getSettlementAndSettlementItems(
      @PathVariable("reservationId") UUID reservationId, HttpServletRequest request) {
    String token = request.getHeader(AUTHORIZATION_HEADER);

    ReservationDto reservation = stripeService.getReservation(reservationId, token);
    authorizeAccess(token, reservation);

    SettlementResponseDto response = paymentService.getSettlementWithItems(reservationId, token);

    return ResponseEntity.ok(response);
  }

  @GetMapping("settlements/{reservationId}/{settlementItemType}")
  public ResponseEntity<SettlementItemDto> getSettlementItemInfoByType(
      @PathVariable("reservationId") UUID reservationId,
      @PathVariable("settlementItemType") SettlementItemType settlementItemType,
      HttpServletRequest request) {
    String token = request.getHeader(AUTHORIZATION_HEADER);

    ReservationDto reservation = stripeService.getReservation(reservationId, token);
    authorizeAccess(token, reservation);

    SettlementItemDto response =
        paymentService.getSettlementItemInfoByType(reservationId, settlementItemType, token);

    return ResponseEntity.ok(response);
  }

  private void authorizeAccess(String authHeader, ReservationDto reservation) {
    if (reservation == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    boolean isManager =
        authentication.getAuthorities().stream()
            .anyMatch(
                authority ->
                    authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_OWNER"));

    if (isManager) {
      return;
    }

    String loggedInUserId = extractUserId(authHeader);
    if (loggedInUserId == null
        || reservation.getUserId() == null
        || !reservation.getUserId().toString().equals(loggedInUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
  }

  private String extractUserId(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    String token = authHeader.substring(7);
    try {
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
              .build()
              .parseClaimsJws(token)
              .getBody();

      String userId = claims.get("userId", String.class);
      if (userId == null || userId.isBlank()) {
        return claims.getSubject();
      }
      return userId;
    } catch (Exception e) {
      return null;
    }
  }
}
