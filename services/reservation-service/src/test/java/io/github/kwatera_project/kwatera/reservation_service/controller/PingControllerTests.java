package io.github.kwatera_project.kwatera.reservation_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.kwatera_project.kwatera.reservation_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.reservation_service.filter.JwtAuthFilter;
import io.github.kwatera_project.kwatera.reservation_service.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PingController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
public class PingControllerTests {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private JwtService jwtService;

  @Test
  void shouldReturnPingResponse() throws Exception {

    mockMvc
        .perform(get("/api/ping"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(jsonPath("$.service").value("reservation-service"))
        .andExpect(jsonPath("$.project").value("KWATERA"));
  }
}
