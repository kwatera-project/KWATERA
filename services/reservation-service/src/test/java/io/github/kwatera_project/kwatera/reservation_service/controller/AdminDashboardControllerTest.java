package io.github.kwatera_project.kwatera.reservation_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.reservation_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.reservation_service.dto.ReservationMetricsDto;
import io.github.kwatera_project.kwatera.reservation_service.filter.JwtAuthFilter;
import io.github.kwatera_project.kwatera.reservation_service.service.JwtService;
import io.github.kwatera_project.kwatera.reservation_service.service.ReservationService;
import java.time.LocalDate;
import java.util.Arrays;
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

@WebMvcTest(AdminDashboardController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AdminDashboardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReservationService reservationService;

  @MockitoBean private JwtService jwtService;

  private Authentication buildAuth(UUID userId, String... roles) {
    List<GrantedAuthority> authorities =
        Arrays.stream(roles).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("admin@test.com", null, authorities);
    auth.setDetails(userId != null ? userId.toString() : null);
    return auth;
  }

  @Test
  void shouldReturnMetrics_whenValidAdminTokenProvided() throws Exception {
    UUID ownerId = UUID.randomUUID();
    ReservationMetricsDto mockDto = new ReservationMetricsDto(10L, 75.0, 15L);

    when(reservationService.getDashboardReservationMetrics(
            any(LocalDate.class), any(LocalDate.class), eq(ownerId), eq(true)))
        .thenReturn(mockDto);

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/reservations")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-31")
                .with(authentication(buildAuth(ownerId, "ROLE_ADMIN"))))
        .andExpect(status().isOk());

    verify(reservationService)
        .getDashboardReservationMetrics(
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ownerId, true);
  }

  @Test
  void shouldReturnMetrics_whenValidOwnerTokenProvided() throws Exception {
    UUID ownerId = UUID.randomUUID();
    ReservationMetricsDto mockDto = new ReservationMetricsDto(5L, 50.0, 10L);

    when(reservationService.getDashboardReservationMetrics(
            any(LocalDate.class), any(LocalDate.class), eq(ownerId), eq(false)))
        .thenReturn(mockDto);

    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/reservations")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-31")
                .with(authentication(buildAuth(ownerId, "ROLE_OWNER"))))
        .andExpect(status().isOk());

    verify(reservationService)
        .getDashboardReservationMetrics(
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), ownerId, false);
  }

  @Test
  void shouldFail_whenUserIsGuest() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/dashboard/reservations")
                .with(authentication(buildAuth(UUID.randomUUID(), "ROLE_GUEST"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldFail_whenNoTokenProvided() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/dashboard/reservations"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenInvalidUuidTokenProvided() throws Exception {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "admin@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    auth.setDetails("invalid-uuid-string");

    mockMvc
        .perform(get("/api/v1/admin/dashboard/reservations").with(authentication(auth)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenEmptyTokenDetailsProvided() throws Exception {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "admin@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    auth.setDetails("   ");

    mockMvc
        .perform(get("/api/v1/admin/dashboard/reservations").with(authentication(auth)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldFail_whenNonStringDetailsProvided() throws Exception {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "admin@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    auth.setDetails(12345); // Integer instead of String

    mockMvc
        .perform(get("/api/v1/admin/dashboard/reservations").with(authentication(auth)))
        .andExpect(status().isUnauthorized());
  }
}
