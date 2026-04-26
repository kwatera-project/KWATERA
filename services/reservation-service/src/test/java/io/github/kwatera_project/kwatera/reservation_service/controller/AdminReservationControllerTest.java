package io.github.kwatera_project.kwatera.reservation_service.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.reservation_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.reservation_service.filter.JwtAuthFilter;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.service.AdminReservationService;
import io.github.kwatera_project.kwatera.reservation_service.service.JwtService;
import java.util.Arrays;
import java.util.Collections;
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

@WebMvcTest(AdminReservationController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AdminReservationControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private AdminReservationController adminReservationController;

  @MockitoBean private AdminReservationService adminReservationService;

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
  void shouldReturnReservations_whenValidAdminTokenProvided() throws Exception {
    UUID ownerId = UUID.randomUUID();
    when(adminReservationService.getReservationsOverview(
            ownerId, ReservationStatus.CONFIRMED, true))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(
            get("/api/v1/admin/reservations")
                .param("status", "CONFIRMED")
                .with(authentication(buildAuth(ownerId, "ROLE_ADMIN"))))
        .andExpect(status().isOk());

    verify(adminReservationService)
        .getReservationsOverview(ownerId, ReservationStatus.CONFIRMED, true);
  }

  @Test
  void shouldUpdateStatus_whenValidAdminTokenProvided() throws Exception {
    UUID resId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String json = "{\"newStatus\":\"CONFIRMED\"}";

    mockMvc
        .perform(
            patch("/api/v1/admin/reservations/" + resId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildAuth(userId, "ROLE_ADMIN"))))
        .andExpect(status().isOk());

    verify(adminReservationService)
        .updateReservationStatus(eq(resId), eq(ReservationStatus.CONFIRMED), eq(userId), eq(true));
  }

  @Test
  void shouldUpdateStatus_whenValidOwnerTokenProvided() throws Exception {
    UUID resId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String json = "{\"newStatus\":\"CANCELLED\"}";

    mockMvc
        .perform(
            patch("/api/v1/admin/reservations/" + resId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(authentication(buildAuth(userId, "ROLE_OWNER"))))
        .andExpect(status().isOk());

    verify(adminReservationService)
        .updateReservationStatus(eq(resId), eq(ReservationStatus.CANCELLED), eq(userId), eq(false));
  }

  @Test
  void shouldFailPatch_whenTokenIsMissing() throws Exception {
    mockMvc
        .perform(patch("/api/v1/admin/reservations/" + UUID.randomUUID() + "/status"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFailPatch_whenUserHasGuestRole() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/admin/reservations/" + UUID.randomUUID() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newStatus\":\"CONFIRMED\"}")
                .with(authentication(buildAuth(UUID.randomUUID(), "ROLE_GUEST"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldFail_whenAuthorizationHeaderIsMissing() throws Exception {
    mockMvc.perform(get("/api/v1/admin/reservations")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenTokenIsMalformed() throws Exception {
    String token = "invalid-token";
    when(jwtService.isValid(token)).thenReturn(false);

    mockMvc
        .perform(get("/api/v1/admin/reservations").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenUserIdIsMissingFromToken() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/reservations").with(authentication(buildAuth(null, "ROLE_ADMIN"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenUserHasGuestRoleGet() throws Exception {
    UUID ownerId = UUID.randomUUID();
    mockMvc
        .perform(
            get("/api/v1/admin/reservations")
                .with(authentication(buildAuth(ownerId, "ROLE_GUEST"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldThrowUnauthorized_whenAuthenticationIsNullDirectly() {
    org.springframework.web.server.ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> adminReservationController.getReservations(null, null));
    org.junit.jupiter.api.Assertions.assertEquals(
        org.springframework.http.HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void shouldThrowUnauthorized_whenAuthenticationDetailsAreNullDirectly() {
    Authentication auth =
        new UsernamePasswordAuthenticationToken(
            "user", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    org.springframework.web.server.ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> adminReservationController.getReservations(null, auth));
    org.junit.jupiter.api.Assertions.assertEquals(
        org.springframework.http.HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void shouldThrowUnauthorized_whenAuthenticationDetailsAreBlank() {
    Authentication auth =
        new UsernamePasswordAuthenticationToken(
            "user", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    ((UsernamePasswordAuthenticationToken) auth).setDetails("   ");
    org.springframework.web.server.ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> adminReservationController.getReservations(null, auth));
    org.junit.jupiter.api.Assertions.assertEquals(
        org.springframework.http.HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  @Test
  void shouldThrowUnauthorized_whenAuthenticationDetailsAreNotString() {
    Authentication auth =
        new UsernamePasswordAuthenticationToken(
            "user", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    ((UsernamePasswordAuthenticationToken) auth).setDetails(12345);
    org.springframework.web.server.ResponseStatusException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> adminReservationController.getReservations(null, auth));
    org.junit.jupiter.api.Assertions.assertEquals(
        org.springframework.http.HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }
}
