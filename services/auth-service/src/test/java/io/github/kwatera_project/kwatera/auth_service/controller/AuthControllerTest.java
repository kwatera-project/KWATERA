package io.github.kwatera_project.kwatera.auth_service.controller;

import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.auth_service.dto.LoginRequest;
import io.github.kwatera_project.kwatera.auth_service.dto.RegisterRequest;
import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import io.github.kwatera_project.kwatera.auth_service.service.JwtService;
import io.github.kwatera_project.kwatera.auth_service.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class
    })
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  @MockitoBean private AuthenticationManager authenticationManager;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private UserRepository userRepository;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldRegisterUser() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("jan");
    request.setEmail("jan@test.com");
    request.setPassword("123");
    request.setRole(Role.GUEST);
    request.setFirstName("Jan");
    request.setLastName("Kowalski");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("User registered"));

    verify(userService).register("jan", "jan@test.com", Role.GUEST, "123", "Jan", "Kowalski");
  }

  @Test
  void shouldReturnConflictWhenUserExists() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("jan");
    request.setEmail("jan@test.com");
    request.setPassword("123");
    request.setRole(Role.GUEST);
    request.setFirstName("Jan");
    request.setLastName("Kowalski");

    doThrow(new ResponseStatusException(CONFLICT, "User already exists"))
        .when(userService)
        .register("jan", "jan@test.com", Role.GUEST, "123", "Jan", "Kowalski");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldReturnBadRequestWhenInvalidJson() throws Exception {
    String invalidJson = "{ invalid json }";

    mockMvc
        .perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400_whenUsernameIsBlank() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("");
    request.setEmail("test@test.com");
    request.setPassword("123456");
    request.setRole(Role.GUEST);
    request.setFirstName("Jan");
    request.setLastName("Kowalski");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verify(userService, never()).register(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldReturn400_whenEmailInvalid() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("test");
    request.setEmail("not-email");
    request.setPassword("123456");
    request.setRole(Role.GUEST);
    request.setFirstName("Jan");
    request.setLastName("Kowalski");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400_whenRoleIsNull() throws Exception {
    RegisterRequest request = new RegisterRequest();
    request.setUsername("test");
    request.setEmail("test@test.com");
    request.setPassword("123456");
    request.setRole(null);
    request.setFirstName("Jan");
    request.setLastName("Kowalski");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldLoginSuccessfully() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setEmail("test@test.com");
    request.setPassword("password");

    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("test");
    user.setEmail("test@test.com");
    user.setRole(Role.GUEST);

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);

    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

    when(jwtService.generateToken(anyMap(), any(UserDetails.class))).thenReturn("jwt-token");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"token\":\"jwt-token\"}"));

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(userRepository).findByEmail("test@test.com");
    verify(jwtService).generateToken(anyMap(), any(UserDetails.class));
  }

  @Test
  void shouldReturnUnauthorized_whenCredentialsInvalid() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setEmail("test@test.com");
    request.setPassword("wrong");

    when(authenticationManager.authenticate(any()))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid credentials"));

    verify(jwtService, never()).generateToken(any());
  }
}
