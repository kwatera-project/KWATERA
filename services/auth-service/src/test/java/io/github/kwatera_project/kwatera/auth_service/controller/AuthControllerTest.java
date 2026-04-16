package io.github.kwatera_project.kwatera.auth_service.controller;

import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.auth_service.dto.RegisterRequest;
import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
      org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
      org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
          .class
    })
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UserService userService;

  @Autowired private ObjectMapper objectMapper;

  // ✅ SUCCESS
  @Test
  void shouldRegisterUser() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("jan");
    request.setEmail("jan@test.com");
    request.setPassword("123");
    request.setRole(Role.GUEST);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("User registered"));

    verify(userService).register("jan", "jan@test.com", Role.GUEST, "123");
  }

  // ❌ USER EXISTS → 409
  @Test
  void shouldReturnConflictWhenUserExists() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("jan");
    request.setEmail("jan@test.com");
    request.setPassword("123");
    request.setRole(Role.GUEST);

    doThrow(new ResponseStatusException(CONFLICT, "User already exists"))
        .when(userService)
        .register("jan", "jan@test.com", Role.GUEST, "123");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  // ❌ INVALID JSON → 400
  @Test
  void shouldReturnBadRequestWhenInvalidJson() throws Exception {
    String invalidJson = "{ invalid json }";

    mockMvc
        .perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  // ❌ BLANK USERNAME FIELD → 400
  @Test
  void shouldReturn400_whenUsernameIsBlank() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername(""); // ❌ invalid
    request.setEmail("test@test.com");
    request.setPassword("123456");
    request.setRole(Role.GUEST);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(userService, never()).register(any(), any(), any(), any());
  }

  // ❌ INVALID EMAIL FIELD → 400
  @Test
  void shouldReturn400_whenEmailInvalid() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("test");
    request.setEmail("not-email"); // ❌ invalid
    request.setPassword("123456");
    request.setRole(Role.GUEST);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ❌ NULL ROLE FIELD → 400
  @Test
  void shouldReturn400_whenRoleIsNull() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("test");
    request.setEmail("test@test.com");
    request.setPassword("123456");
    request.setRole(null); // ❌

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
