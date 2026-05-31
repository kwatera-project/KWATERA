package io.github.kwatera_project.kwatera.property_service.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kwatera_project.kwatera.property_service.filter.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(properties = {"jwt.secret=supersecretkeysupersecretkey123456"})
@AutoConfigureMockMvc
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private JwtAuthFilter jwtAuthFilter;

  @Autowired ApplicationContext context;

  @Test
  void printChains() {
    System.out.println(context.getBeansOfType(SecurityFilterChain.class));
  }

  @Test
  void shouldAllowPublicEndpointsWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/public")).andExpect(status().isOk());
  }

  @Test
  void shouldAllowOwnerEndpointForUserWithOwnerRole() throws Exception {
    mockMvc
        .perform(get("/api/owner/test").with(user("owner").authorities(() -> "ROLE_OWNER")))
        .andExpect(status().isOk());
  }

  @Test
  void shouldAllowOptionsRequests() throws Exception {
    mockMvc
        .perform(
            options("/api/owner/test")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", HttpMethod.GET.name()))
        .andExpect(status().isOk());
  }

  @RestController
  static class TestController {

    @GetMapping("/public")
    public String publicEndpoint() {
      return "ok";
    }

    @GetMapping("/api/owner/test")
    public String ownerEndpoint() {
      return "ok";
    }
  }
}
