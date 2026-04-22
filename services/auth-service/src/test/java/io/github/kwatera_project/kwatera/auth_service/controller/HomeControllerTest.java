package io.github.kwatera_project.kwatera.auth_service.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.kwatera_project.kwatera.auth_service.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

  @MockitoBean private JwtService jwtService;

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldReturnOkForAuthenticatedUser() throws Exception {
    mockMvc
        .perform(get("/").with(user("test@mail.com").roles("GUEST")))
        .andExpect(status().isOk())
        .andExpect(content().string("OK - logged in"));
  }
}
