package io.github.kwatera_project.kwatera.auth_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.kwatera_project.kwatera.auth_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.auth_service.config.SecurityConfig;
import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import io.github.kwatera_project.kwatera.auth_service.service.JwtService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserRepository userRepository;
  @MockitoBean private PropertyClient propertyClient;
  @MockitoBean private AuthenticationManager authenticationManager;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;

  @Test
  void shouldReturnKpisForAdmin() throws Exception {
    when(userRepository.count()).thenReturn(10L);
    when(userRepository.countByRole(Role.GUEST)).thenReturn(6L);
    when(userRepository.countByRole(Role.OWNER)).thenReturn(3L);
    when(propertyClient.getTotalPropertiesCount()).thenReturn(5L);

    mockMvc
        .perform(
            get("/api/admin/users/kpis")
                .with(user("admin@mail.com").roles("ADMIN"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsers").value(10))
        .andExpect(jsonPath("$.totalGuests").value(6))
        .andExpect(jsonPath("$.totalOwners").value(3))
        .andExpect(jsonPath("$.totalProperties").value(5));

    verify(userRepository).count();
    verify(userRepository).countByRole(Role.GUEST);
    verify(userRepository).countByRole(Role.OWNER);
    verify(propertyClient).getTotalPropertiesCount();
  }

  @Test
  void shouldDenyKpisForNonAdmin() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/users/kpis")
                .with(user("owner@mail.com").roles("OWNER"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnUsersPageForAdmin() throws Exception {
    UUID guestId = UUID.randomUUID();
    User guest = new User();
    guest.setId(guestId);
    guest.setFirstName("John");
    guest.setLastName("Guest");
    guest.setEmail("john@guest.com");
    guest.setRole(Role.GUEST);
    guest.setCreatedAt(Instant.now());

    UUID ownerId = UUID.randomUUID();
    User owner = new User();
    owner.setId(ownerId);
    owner.setFirstName("Marcus");
    owner.setLastName("Owner");
    owner.setEmail("marcus@owner.com");
    owner.setRole(Role.OWNER);
    owner.setCreatedAt(Instant.now());

    List<User> users = List.of(guest, owner);
    PageImpl<User> userPage = new PageImpl<>(users);

    when(userRepository.findAllFilteredAndSearched(
            eq(Role.OWNER), eq("search"), any(Pageable.class)))
        .thenReturn(userPage);
    when(propertyClient.getOwnerPropertyCounts(List.of(ownerId))).thenReturn(Map.of(ownerId, 3L));

    mockMvc
        .perform(
            get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .param("role", "OWNER")
                .param("search", "search")
                .with(user("admin@mail.com").roles("ADMIN"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].firstName").value("John"))
        .andExpect(jsonPath("$.content[1].firstName").value("Marcus"))
        .andExpect(jsonPath("$.content[1].propertyCount").value(3));

    verify(userRepository)
        .findAllFilteredAndSearched(eq(Role.OWNER), eq("search"), any(Pageable.class));
    verify(propertyClient).getOwnerPropertyCounts(List.of(ownerId));
  }

  @Test
  void shouldDenyUsersPageForNonAdmin() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/users")
                .with(user("guest@mail.com").roles("GUEST"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }
}
