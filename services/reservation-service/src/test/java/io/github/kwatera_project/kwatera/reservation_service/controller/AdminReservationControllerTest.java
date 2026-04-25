package io.github.kwatera_project.kwatera.reservation_service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import io.github.kwatera_project.kwatera.reservation_service.service.AdminReservationService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminReservationController.class)
class AdminReservationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AdminReservationService adminReservationService;

  private String createMockToken(UUID userId, String role) {
    String payload = "{";
    if (userId != null) {
      payload += "\"userId\":\"" + userId + "\"";
    }
    if (role != null) {
      if (userId != null) payload += ",";
      payload += "\"role\":[\"" + role + "\"]";
    }
    payload += "}";

    String header =
        Base64.getUrlEncoder()
            .encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
    String encodedPayload =
        Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));

    return header + "." + encodedPayload + ".signature";
  }

  @Test
  void shouldReturnReservations_whenValidAdminTokenProvided() throws Exception {
    UUID ownerId = UUID.randomUUID();
    String token = createMockToken(ownerId, "ROLE_ADMIN");

    when(adminReservationService.getReservationsOverview(
            ownerId, ReservationStatus.CONFIRMED, true))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(
            get("/api/v1/admin/reservations")
                .param("status", "CONFIRMED")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    verify(adminReservationService)
        .getReservationsOverview(ownerId, ReservationStatus.CONFIRMED, true);
  }

  @Test
  void shouldReturnReservations_whenValidOwnerTokenProvided() throws Exception {
    UUID ownerId = UUID.randomUUID();
    String token = createMockToken(ownerId, "ROLE_OWNER");

    when(adminReservationService.getReservationsOverview(ownerId, null, false))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/v1/admin/reservations").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    verify(adminReservationService).getReservationsOverview(ownerId, null, false);
  }

  @Test
  void shouldFail_whenAuthorizationHeaderIsMissing() throws Exception {
    mockMvc.perform(get("/api/v1/admin/reservations")).andExpect(status().isInternalServerError());
  }

  @Test
  void shouldFail_whenTokenIsMalformed() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/reservations")
                .header("Authorization", "Bearer to_nie_jest_poprawny_token.JWT"))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void shouldFail_whenUserIdIsMissingFromToken() throws Exception {
    String token = createMockToken(null, "ROLE_ADMIN");

    mockMvc
        .perform(get("/api/v1/admin/reservations").header("Authorization", "Bearer " + token))
        .andExpect(status().isInternalServerError());
  }
}
