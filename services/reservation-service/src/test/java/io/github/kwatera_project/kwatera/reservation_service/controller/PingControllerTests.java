package io.github.kwatera_project.kwatera.reservation_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PingController.class)
public class PingControllerTests {
  @Autowired private MockMvc mockMvc;

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
