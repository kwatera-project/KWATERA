package io.github.kwatera_project.kwatera.reservation_service.audit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.reservation_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.reservation_service.filter.JwtAuthFilter;
import io.github.kwatera_project.kwatera.reservation_service.service.JwtService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SystemEventController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SystemEventControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SystemEventService systemEventService;

  @MockitoBean private JwtService jwtService;

  private Authentication buildAuth(String role) {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "user@test.com", null, List.of(new SimpleGrantedAuthority(role)));
    auth.setDetails(UUID.randomUUID().toString());
    return auth;
  }

  @Test
  void shouldAllowAdminToAccessSystemEvents() throws Exception {
    when(systemEventService.getLatestEvents(null, null, null, null)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/admin/system-events").with(authentication(buildAuth("ROLE_ADMIN"))))
        .andExpect(status().isOk());

    verify(systemEventService).getLatestEvents(null, null, null, null);
  }

  @Test
  void shouldPassActionTypeLimitAndTimestampRangeToServiceAndSerializeResponse() throws Exception {
    UUID id = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();
    Instant from = Instant.parse("2026-06-10T00:00:00Z");
    Instant to = Instant.parse("2026-06-10T23:59:59Z");
    SystemEventResponseDto response =
        new SystemEventResponseDto(
            id,
            Instant.parse("2026-06-10T12:00:00Z"),
            SystemEventType.UNIT_BLOCKED,
            actorUserId,
            "RESERVATION",
            entityId,
            "reservationId=" + entityId + ", status=BLOCKED");
    when(systemEventService.getLatestEvents(SystemEventType.UNIT_BLOCKED, 50, from, to))
        .thenReturn(List.of(response));

    mockMvc
        .perform(
            get("/api/v1/admin/system-events")
                .param("actionType", "UNIT_BLOCKED")
                .param("limit", "50")
                .param("from", "2026-06-10T00:00:00Z")
                .param("to", "2026-06-10T23:59:59Z")
                .with(authentication(buildAuth("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].timestamp").value("2026-06-10T12:00:00Z"))
        .andExpect(jsonPath("$[0].actionType").value("UNIT_BLOCKED"))
        .andExpect(jsonPath("$[0].actorUserId").value(actorUserId.toString()))
        .andExpect(jsonPath("$[0].entityType").value("RESERVATION"))
        .andExpect(jsonPath("$[0].entityId").value(entityId.toString()))
        .andExpect(
            jsonPath("$[0].details").value("reservationId=" + entityId + ", status=BLOCKED"));

    verify(systemEventService).getLatestEvents(SystemEventType.UNIT_BLOCKED, 50, from, to);
  }

  @Test
  void shouldRejectOwnerFromSystemEvents() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/system-events").with(authentication(buildAuth("ROLE_OWNER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRejectGuestFromSystemEvents() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/system-events").with(authentication(buildAuth("ROLE_GUEST"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRejectMissingTokenFromSystemEvents() throws Exception {
    mockMvc.perform(get("/api/v1/admin/system-events")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectInvalidTokenFromSystemEvents() throws Exception {
    String token = "invalid-token";
    when(jwtService.isValid(token)).thenReturn(false);

    mockMvc
        .perform(get("/api/v1/admin/system-events").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldAcceptInternalSystemEventWithValidToken() throws Exception {
    UUID actorUserId = UUID.randomUUID();
    UUID entityId = UUID.randomUUID();
    String body =
        """
        {
          "actionType": "PAYMENT_FAILED",
          "actorUserId": "%s",
          "entityType": "SETTLEMENT",
          "entityId": "%s",
          "details": "settlementId=%s, reason=Checkout payment failed"
        }
        """
            .formatted(actorUserId, entityId, entityId);

    mockMvc
        .perform(
            post("/api/v1/internal/system-events")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Token", "kwatera-internal-secret-token")
                .content(body))
        .andExpect(status().isOk());

    verify(systemEventService)
        .logSafely(
            SystemEventType.PAYMENT_FAILED,
            actorUserId,
            "SETTLEMENT",
            entityId,
            "settlementId=" + entityId + ", reason=Checkout payment failed");
  }

  @Test
  void shouldRejectInternalSystemEventWhenTokenIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/internal/system-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actionType\":\"BALANCE_CHANGED\",\"entityType\":\"SETTLEMENT\"}"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(systemEventService);
  }

  @Test
  void shouldRejectInternalSystemEventWhenTokenIsWrong() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/internal/system-events")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Token", "wrong-token")
                .content("{\"actionType\":\"BALANCE_CHANGED\",\"entityType\":\"SETTLEMENT\"}"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(systemEventService);
  }
}
