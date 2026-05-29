package io.github.kwatera_project.kwatera.billing_service.service;

import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MediaReadingAccessService {

  private final SettlementRepository settlementRepository;
  private final RestTemplate restTemplate;

  public void validateReadAccess(UUID settlementId, Authentication authentication, String token) {
    if (hasAuthority(authentication, "ROLE_GUEST")) {
      validateGuestAccess(settlementId, authentication, token);
      return;
    }

    validateReviewerAccess(settlementId, authentication, token);
  }

  public void validateGuestAccess(UUID settlementId, Authentication authentication, String token) {
    requireAuthority(authentication, "ROLE_GUEST");

    ReservationDto reservation = fetchReservationForSettlement(settlementId, token);
    UUID authenticatedUserId = authenticatedUserId(authentication);

    if (reservation.getUserId() == null || !reservation.getUserId().equals(authenticatedUserId)) {
      throw new AccessDeniedException("Access denied");
    }
  }

  public void validateReviewerAccess(
      UUID settlementId, Authentication authentication, String token) {
    if (!hasAuthority(authentication, "ROLE_ADMIN")
        && !hasAuthority(authentication, "ROLE_OWNER")) {
      throw new AccessDeniedException("Access denied");
    }

    // Reservation-service validates admin/global access and owner access to the reservation unit.
    fetchReservationForSettlement(settlementId, token);
  }

  private ReservationDto fetchReservationForSettlement(UUID settlementId, String token) {
    Settlement settlement =
        settlementRepository
            .findById(settlementId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Settlement not found"));

    String url = "http://reservation-service/api/v1/reservations/" + settlement.getReservationId();
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", normalizeToken(token));

    try {
      ResponseEntity<ReservationDto> response =
          restTemplate.exchange(
              url, HttpMethod.GET, new HttpEntity<>(headers), ReservationDto.class);

      if (response.getBody() == null) {
        throw new AccessDeniedException("Access denied");
      }

      return response.getBody();
    } catch (AccessDeniedException e) {
      throw e;
    } catch (ResponseStatusException e) {
      if (e.getStatusCode().is4xxClientError()) {
        throw new AccessDeniedException("Access denied", e);
      }
      throw e;
    } catch (Exception e) {
      throw new AccessDeniedException("Access denied", e);
    }
  }

  private String normalizeToken(String token) {
    if (token == null || token.isBlank()) {
      throw new AccessDeniedException("Access denied");
    }
    return token.startsWith("Bearer ") ? token : "Bearer " + token;
  }

  private UUID authenticatedUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Access denied");
    }

    Object details = authentication.getDetails();
    if (!(details instanceof String userId) || userId.isBlank()) {
      throw new AccessDeniedException("Access denied");
    }

    try {
      return UUID.fromString(userId);
    } catch (IllegalArgumentException e) {
      throw new AccessDeniedException("Access denied", e);
    }
  }

  private void requireAuthority(Authentication authentication, String authority) {
    if (!hasAuthority(authentication, authority)) {
      throw new AccessDeniedException("Access denied");
    }
  }

  private boolean hasAuthority(Authentication authentication, String authority) {
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
  }
}
