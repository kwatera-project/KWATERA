package io.github.kwatera_project.kwatera.auth_service.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.kwatera_project.kwatera.auth_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.auth_service.dto.UserProfileDto;
import io.github.kwatera_project.kwatera.auth_service.mapper.UserMapper;
import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.service.JwtService;
import io.github.kwatera_project.kwatera.auth_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserProfileController.class)
@Import(SecurityConfig.class)
class UserProfileControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  @MockitoBean private AuthenticationManager authenticationManager;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private UserDetailsService userDetailsService;

  @MockitoBean private UserMapper userMapper;

  @Test
  void shouldReturnUserProfile() throws Exception {
    // Given
    String email = "test@mail.com";
    String username = "test";
    java.util.UUID id = java.util.UUID.randomUUID();

    User user = new User();
    user.setId(id);
    user.setUsername(username);
    user.setEmail(email);
    user.setFirstName("First");
    user.setLastName("Last");
    user.setRole(Role.GUEST);

    UserProfileDto dto = new UserProfileDto(username, "First", "Last", email, Role.GUEST);

    when(userService.getUserByEmail(email)).thenReturn(user);
    when(userMapper.toUserProfileDto(user)).thenReturn(dto);

    // When + Then
    mockMvc
        .perform(
            get("/api/auth/users/me")
                .with(user(email).roles("GUEST"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.username").value(username))
        .andExpect(jsonPath("$.firstName").value("First"))
        .andExpect(jsonPath("$.lastName").value("Last"))
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value("GUEST"));

    verify(userService).getUserByEmail(email);
    verify(userMapper).toUserProfileDto(user);
  }

  @Test
  void shouldUpdateUserProfile() throws Exception {
    // Given
    String email = "test@mail.com";
    String username = "test";
    java.util.UUID id = java.util.UUID.randomUUID();

    User updatedUser = new User();
    updatedUser.setId(id);
    updatedUser.setUsername(username);
    updatedUser.setEmail(email);
    updatedUser.setFirstName("NewFirst");
    updatedUser.setLastName("NewLast");
    updatedUser.setRole(Role.GUEST);

    UserProfileDto dto = new UserProfileDto(username, "NewFirst", "NewLast", email, Role.GUEST);

    when(userService.updateProfile(email, "NewFirst", "NewLast")).thenReturn(updatedUser);
    when(userMapper.toUserProfileDto(updatedUser)).thenReturn(dto);

    // When + Then
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/auth/users/me")
                .with(user(email).roles("GUEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"NewFirst\",\"lastName\":\"NewLast\"}")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.username").value(username))
        .andExpect(jsonPath("$.firstName").value("NewFirst"))
        .andExpect(jsonPath("$.lastName").value("NewLast"))
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value("GUEST"));

    verify(userService).updateProfile(email, "NewFirst", "NewLast");
    verify(userMapper).toUserProfileDto(updatedUser);
  }

  @Test
  void shouldRejectUnauthenticatedGetUserProfile() throws Exception {
    mockMvc
        .perform(get("/api/auth/users/me").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRejectUnauthenticatedUpdateUserProfile() throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/auth/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"NewFirst\",\"lastName\":\"NewLast\"}")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnUserProfileInternal_withToken() throws Exception {
    java.util.UUID id = java.util.UUID.randomUUID();
    User user = new User();
    user.setId(id);
    user.setUsername("test");
    user.setEmail("test@mail.com");
    user.setFirstName("First");
    user.setLastName("Last");
    user.setRole(Role.GUEST);

    UserProfileDto dto = new UserProfileDto("test", "First", "Last", "test@mail.com", Role.GUEST);

    when(userService.getUserById(id)).thenReturn(user);
    when(userMapper.toUserProfileDto(user)).thenReturn(dto);

    mockMvc
        .perform(
            get("/api/auth/users/internal/" + id)
                .header("X-Internal-Token", "kwatera-internal-secret-token")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.username").value("test"))
        .andExpect(jsonPath("$.firstName").value("First"))
        .andExpect(jsonPath("$.lastName").value("Last"))
        .andExpect(jsonPath("$.email").value("test@mail.com"))
        .andExpect(jsonPath("$.role").value("GUEST"));

    verify(userService).getUserById(id);
    verify(userMapper).toUserProfileDto(user);
  }

  @Test
  void shouldRejectUserProfileInternal_withoutToken() throws Exception {
    java.util.UUID id = java.util.UUID.randomUUID();
    mockMvc
        .perform(get("/api/auth/users/internal/" + id).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }
}
