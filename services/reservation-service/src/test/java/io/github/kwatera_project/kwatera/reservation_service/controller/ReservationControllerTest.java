package io.github.kwatera_project.kwatera.reservation_service.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.reservation_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.reservation_service.dto.CreateReservationRequest;
import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationDetailsDto;
import io.github.kwatera_project.kwatera.reservation_service.filter.JwtAuthFilter;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.service.JwtService;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class ReservationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReservationService reservationService;

  @MockitoBean private JwtService jwtService;

  private Authentication buildAuth(UUID userId, String... roles) {
    List<GrantedAuthority> authorities =
        Arrays.stream(roles).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("user@test.com", null, authorities);
    auth.setDetails(userId != null ? userId.toString() : null);
    return auth;
  }

  @Test
  void shouldCreateReservation_whenValidTokenProvided() throws Exception {
    UUID userId = UUID.randomUUID();
    String json =
        "{\"unitId\":\""
            + UUID.randomUUID()
            + "\", \"startDate\":\"2026-10-10\", \"endDate\":\"2026-10-15\"}";

    when(reservationService.createReservation(
            eq(userId), eq("user@test.com"), any(CreateReservationRequest.class), anyString()))
        .thenReturn(new Reservation());

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildAuth(userId, "ROLE_GUEST"))))
        .andExpect(status().isCreated());

    verify(reservationService)
        .createReservation(
            eq(userId), eq("user@test.com"), any(CreateReservationRequest.class), anyString());
  }

  @Test
  void shouldCreateReservationWithGuestEmailFromRequest_whenProvided() throws Exception {
    UUID userId = UUID.randomUUID();
    String json =
        "{\"unitId\":\""
            + UUID.randomUUID()
            + "\", \"startDate\":\"2026-10-10\", \"endDate\":\"2026-10-15\", "
            + "\"guestEmail\":\"manual.guest@example.com\"}";

    when(reservationService.createReservation(
            eq(userId),
            eq("manual.guest@example.com"),
            any(CreateReservationRequest.class),
            anyString()))
        .thenReturn(new Reservation());

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildAuth(userId, "ROLE_OWNER"))))
        .andExpect(status().isCreated());

    verify(reservationService)
        .createReservation(
            eq(userId),
            eq("manual.guest@example.com"),
            any(CreateReservationRequest.class),
            eq("Bearer mock-token"));
  }

  @Test
  void shouldFallbackToAuthenticatedEmail_whenGuestEmailIsBlank() throws Exception {
    UUID userId = UUID.randomUUID();
    String json =
        "{\"unitId\":\""
            + UUID.randomUUID()
            + "\", \"startDate\":\"2026-10-10\", \"endDate\":\"2026-10-15\", "
            + "\"guestEmail\":\"   \"}";

    when(reservationService.createReservation(
            eq(userId), eq("user@test.com"), any(CreateReservationRequest.class), anyString()))
        .thenReturn(new Reservation());

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .header("Authorization", "Bearer mock-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildAuth(userId, "ROLE_OWNER"))))
        .andExpect(status().isCreated());

    verify(reservationService)
        .createReservation(
            eq(userId),
            eq("user@test.com"),
            any(CreateReservationRequest.class),
            eq("Bearer mock-token"));
  }

  @Test
  void shouldFail_whenTokenIsMissing() throws Exception {
    mockMvc
        .perform(post("/api/v1/reservations").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenUserIdIsMissingInToken() throws Exception {
    String json =
        "{\"unitId\":\""
            + UUID.randomUUID()
            + "\", \"startDate\":\"2026-10-10\", \"endDate\":\"2026-10-15\"}";

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildAuth(null, "ROLE_GUEST"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenRequiredFieldsAreMissing() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(authentication(buildAuth(userId, "ROLE_GUEST"))))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(reservationService);
  }

  @Test
  void shouldFail_whenUserIdIsMalformedInToken() throws Exception {
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("user@test.com", null, authorities);
    auth.setDetails("not-a-uuid");

    String json =
        "{\"unitId\":\""
            + UUID.randomUUID()
            + "\", \"startDate\":\"2026-10-10\", \"endDate\":\"2026-10-15\"}";

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(auth)))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(reservationService);
  }

  @Test
  void shouldFail_whenAuthenticationIsNotAuthenticated() throws Exception {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("user@test.com", null, List.of());
    auth.setAuthenticated(false);

    String json =
        "{\"unitId\":\""
            + UUID.randomUUID()
            + "\", \"startDate\":\"2026-10-10\", \"endDate\":\"2026-10-15\"}";

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(auth)))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldFail_whenDetailsAreNotAString() throws Exception {
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("user@test.com", null, authorities);
    auth.setDetails(12345);

    String json =
        "{\"unitId\":\""
            + UUID.randomUUID()
            + "\", \"startDate\":\"2026-10-10\", \"endDate\":\"2026-10-15\"}";

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(auth)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenUserIdIsBlankInToken() throws Exception {
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("user@test.com", null, authorities);
    auth.setDetails("   ");

    String json =
        "{\"unitId\":\""
            + UUID.randomUUID()
            + "\", \"startDate\":\"2026-10-10\", \"endDate\":\"2026-10-15\"}";

    mockMvc
        .perform(
            post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(auth)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldGetReservationDetails_whenUserHasAdminRole() throws Exception {
    UUID adminId = UUID.randomUUID();
    UUID guestId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();

    ReservationDetailsDto dto = new ReservationDetailsDto();
    dto.setId(reservationId);
    dto.setUserId(guestId);

    when(reservationService.getReservationDetails(
            eq(reservationId), eq(adminId), eq(true), eq(false)))
        .thenReturn(dto);

    mockMvc
        .perform(
            get("/api/v1/reservations/" + reservationId)
                .with(authentication(buildAuth(adminId, "ROLE_ADMIN"))))
        .andExpect(status().isOk());

    verify(reservationService)
        .getReservationDetails(eq(reservationId), eq(adminId), eq(true), eq(false));
  }

  @Test
  void shouldFailGetDetails_whenTokenIsMissing() throws Exception {
    mockMvc
        .perform(get("/api/v1/reservations/" + UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFailGetDetails_whenUserIdIsMissingInToken() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/reservations/" + UUID.randomUUID())
                .with(authentication(buildAuth(null, "ROLE_GUEST"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldGetMyReservations_whenUserIsGuest() throws Exception {
    UUID guestId = UUID.randomUUID();

    when(reservationService.getMyReservations(eq(guestId))).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/reservations/my").with(authentication(buildAuth(guestId, "ROLE_GUEST"))))
        .andExpect(status().isOk());

    verify(reservationService).getMyReservations(eq(guestId));
  }

  @Test
  void shouldFailGetMyReservations_whenUserIsNotGuest() throws Exception {
    UUID adminId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/api/v1/reservations/my").with(authentication(buildAuth(adminId, "ROLE_ADMIN"))))
        .andExpect(status().isForbidden());

    verifyNoInteractions(reservationService);
  }

  @Test
  void shouldGetReservationDetailsInternal_withValidInternalToken() throws Exception {
    UUID reservationId = UUID.randomUUID();
    ReservationDetailsDto dto = new ReservationDetailsDto();
    dto.setId(reservationId);

    when(reservationService.getReservationDetailsInternal(reservationId)).thenReturn(dto);

    mockMvc
        .perform(
            get("/api/v1/reservations/internal/" + reservationId)
                .header("X-Internal-Token", "kwatera-internal-secret-token"))
        .andExpect(status().isOk());

    verify(reservationService).getReservationDetailsInternal(reservationId);
  }

  @Test
  void shouldFailGetReservationDetailsInternal_withInvalidInternalToken() throws Exception {
    UUID reservationId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/api/v1/reservations/internal/" + reservationId)
                .header("X-Internal-Token", "wrong-token"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldFailGetReservationDetailsInternal_withMissingInternalToken() throws Exception {
    UUID reservationId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/v1/reservations/internal/" + reservationId))
        .andExpect(status().isForbidden());
  }
}
