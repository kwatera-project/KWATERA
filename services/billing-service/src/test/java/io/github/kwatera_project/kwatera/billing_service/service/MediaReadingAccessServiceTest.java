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
  void shouldAllowGuestAccessWhenReservationUnitMatchesRequestUnit() {
    UUID guestId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    mockSettlementReservation(settlementId, reservationId, guestId, unitId);

    assertDoesNotThrow(
        () ->
            mediaReadingAccessService.validateGuestAccess(
                settlementId, unitId, auth(guestId, "ROLE_GUEST"), TOKEN));
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
  void shouldDenyGuestAccessWhenReservationUnitDiffersFromRequestUnit() {
    UUID guestId = UUID.randomUUID();
    UUID settlementId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    UUID reservationUnitId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID requestUnitId = UUID.fromString("00000000-0000-0000-0000-000000000002");

    mockSettlementReservation(settlementId, reservationId, guestId, reservationUnitId);

    assertThrows(
        AccessDeniedException.class,
        () ->
            mediaReadingAccessService.validateGuestAccess(
                settlementId, requestUnitId, auth(guestId, "ROLE_GUEST"), TOKEN));
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
  void shouldAllowOwnerAndAdminReviewerAccessWhenReservationUnitMatchesRequestUnit() {
    UUID settlementId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    mockSettlementReservation(settlementId, reservationId, guestId, unitId);

    assertDoesNotThrow(
        () ->
            mediaReadingAccessService.validateReviewerAccess(
                settlementId, unitId, auth(ownerId, "ROLE_OWNER"), TOKEN));

    UUID adminSettlementId = UUID.randomUUID();
    UUID adminReservationId = UUID.randomUUID();
    UUID adminUnitId = UUID.randomUUID();
    mockSettlementReservation(adminSettlementId, adminReservationId, guestId, adminUnitId);

    assertDoesNotThrow(
        () ->
            mediaReadingAccessService.validateReviewerAccess(
                adminSettlementId, adminUnitId, auth(UUID.randomUUID(), "ROLE_ADMIN"), TOKEN));
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

  @Test
  void shouldDenyOwnerAndAdminReviewerAccessWhenReservationUnitDiffersFromRequestUnit() {
    UUID ownerSettlementId = UUID.randomUUID();
    UUID ownerReservationId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID ownerReservationUnitId = UUID.fromString("00000000-0000-0000-0000-000000000003");
    UUID ownerRequestUnitId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    mockSettlementReservation(
        ownerSettlementId, ownerReservationId, guestId, ownerReservationUnitId);

    assertThrows(
        AccessDeniedException.class,
        () ->
            mediaReadingAccessService.validateReviewerAccess(
                ownerSettlementId,
                ownerRequestUnitId,
                auth(UUID.randomUUID(), "ROLE_OWNER"),
                TOKEN));

    UUID adminSettlementId = UUID.randomUUID();
    UUID adminReservationId = UUID.randomUUID();
    UUID adminReservationUnitId = UUID.fromString("00000000-0000-0000-0000-000000000005");
    UUID adminRequestUnitId = UUID.fromString("00000000-0000-0000-0000-000000000006");
    mockSettlementReservation(
        adminSettlementId, adminReservationId, guestId, adminReservationUnitId);

    assertThrows(
        AccessDeniedException.class,
        () ->
            mediaReadingAccessService.validateReviewerAccess(
                adminSettlementId,
                adminRequestUnitId,
                auth(UUID.randomUUID(), "ROLE_ADMIN"),
                TOKEN));
  }

  private Authentication auth(UUID userId, String role) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            "user@example.com", null, List.of(new SimpleGrantedAuthority(role)));
    authentication.setDetails(userId.toString());
    return authentication;
  }

  private void mockSettlementReservation(UUID settlementId, UUID reservationId, UUID guestId) {
    mockSettlementReservation(settlementId, reservationId, guestId, UUID.randomUUID());
  }

  private void mockSettlementReservation(
      UUID settlementId, UUID reservationId, UUID guestId, UUID unitId) {
    Settlement settlement = new Settlement();
    settlement.setId(settlementId);
    settlement.setReservationId(reservationId);

    ReservationDto reservation = new ReservationDto();
    reservation.setId(reservationId);
    reservation.setUserId(guestId);
    reservation.setUnitId(unitId);

    when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(settlement));
    when(restTemplate.exchange(
            eq("http://reservation-service/api/v1/reservations/" + reservationId),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(ReservationDto.class)))
        .thenReturn(ResponseEntity.ok(reservation));
  }
}
