package io.github.kwatera_project.kwatera.reservation_service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
  void shouldReturnReservations_whenValidOwnerTokenProvided() throws Exception {
    UUID ownerId = UUID.randomUUID();
    when(adminReservationService.getReservationsOverview(ownerId, null, false))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(
            get("/api/v1/admin/reservations")
                .with(authentication(buildAuth(ownerId, "ROLE_OWNER"))))
        .andExpect(status().isOk());

    verify(adminReservationService).getReservationsOverview(ownerId, null, false);
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
  void shouldFail_whenUserHasGuestRole() throws Exception {
    UUID ownerId = UUID.randomUUID();
    mockMvc
        .perform(
            get("/api/v1/admin/reservations")
                .with(authentication(buildAuth(ownerId, "ROLE_GUEST"))))
        .andExpect(status().isForbidden());
  }
}
