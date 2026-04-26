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

    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setRole(Role.GUEST);

    UserProfileDto dto = new UserProfileDto(email, username);

    when(userService.getUserByEmail(email)).thenReturn(user);
    when(userMapper.toUserProfileDto(user)).thenReturn(dto);

    // When + Then
    mockMvc
        .perform(
            get("/api/user/me").with(user(email).roles("GUEST")).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.email").value(email));

    verify(userService).getUserByEmail(email);
    verify(userMapper).toUserProfileDto(user);
  }
}
