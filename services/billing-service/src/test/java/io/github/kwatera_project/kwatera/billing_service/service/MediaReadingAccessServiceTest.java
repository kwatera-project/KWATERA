package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class MediaReadingAccessServiceTest {

  @Mock private SettlementRepository settlementRepository;
  @Mock private RestTemplate restTemplate;

  @InjectMocks private MediaReadingAccessService mediaReadingAccessService;

  private static final String TOKEN = "Bearer token";

  @Test
  void shouldAllowGuestAccessForOwnSettlementReservation() {
    UUID guestId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();

    mockSettlementReservation(settlementId, reservationId, guestId);

    assertDoesNotThrow(
        () ->
            mediaReadingAccessService.validateGuestAccess(
                settlementId, auth(guestId, "ROLE_GUEST"), TOKEN));
  }

  @Test
  void shouldDenyGuestAccessForAnotherGuestsSettlementReservation() {
    UUID guestId = UUID.randomUUID();
    UUID otherGuestId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();

    mockSettlementReservation(settlementId, reservationId, otherGuestId);

    assertThrows(
        AccessDeniedException.class,
        () ->
            mediaReadingAccessService.validateGuestAccess(
                settlementId, auth(guestId, "ROLE_GUEST"), TOKEN));
  }

  @Test
  void shouldDenyGuestReviewerAccessToUploadAttemptsAndApproval() {
    UUID guestId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();

    assertThrows(
        AccessDeniedException.class,
        () ->
            mediaReadingAccessService.validateReviewerAccess(
                settlementId, auth(guestId, "ROLE_GUEST"), TOKEN));

    verifyNoInteractions(settlementRepository, restTemplate);
  }

  @Test
  void shouldDenyUnauthenticatedGuestAccess() {
    assertThrows(
        AccessDeniedException.class,
        () -> mediaReadingAccessService.validateGuestAccess(UUID.randomUUID(), null, TOKEN));

    verifyNoInteractions(settlementRepository, restTemplate);
  }

  @Test
  void shouldAllowOwnerReviewerAccessWhenReservationServiceAllowsIt() {
    UUID ownerId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();

    mockSettlementReservation(settlementId, reservationId, guestId);

    assertDoesNotThrow(
        () ->
            mediaReadingAccessService.validateReviewerAccess(
                settlementId, auth(ownerId, "ROLE_OWNER"), TOKEN));
  }

  @Test
  void shouldAllowAdminReviewerAccessWhenReservationServiceAllowsIt() {
    UUID adminId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();

    mockSettlementReservation(settlementId, reservationId, guestId);

    assertDoesNotThrow(
        () ->
            mediaReadingAccessService.validateReviewerAccess(
                settlementId, auth(adminId, "ROLE_ADMIN"), TOKEN));
  }

  private Authentication auth(UUID userId, String role) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            "user@example.com", null, List.of(new SimpleGrantedAuthority(role)));
    authentication.setDetails(userId.toString());
    return authentication;
  }

  private void mockSettlementReservation(UUID settlementId, UUID reservationId, UUID guestId) {
    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setReservationId(reservationId);

    ReservationDto reservation = new ReservationDto();
    reservation.setId(reservationId);
    reservation.setUserId(guestId);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
    when(restTemplate.exchange(
            eq("http://reservation-service/api/v1/reservations/" + reservationId),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ReservationDto.class)))
        .thenReturn(ResponseEntity.ok(reservation));
  }
}
