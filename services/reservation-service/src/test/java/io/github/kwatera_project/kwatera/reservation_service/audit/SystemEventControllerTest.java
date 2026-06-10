package io.github.kwatera_project.kwatera.reservation_service.audit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.reservation_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.reservation_service.filter.JwtAuthFilter;
import io.github.kwatera_project.kwatera.reservation_service.service.JwtService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
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
    when(systemEventService.getLatestEvents(null, 100)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/admin/system-events")
                .param("limit", "100")
                .with(authentication(buildAuth("ROLE_ADMIN"))))
        .andExpect(status().isOk());

    verify(systemEventService).getLatestEvents(null, 100);
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
}
