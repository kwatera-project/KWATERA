package io.github.kwatera_project.kwatera.auth_service.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceTest {

  @Mock UserRepository userRepository;

  @InjectMocks SecurityConfig securityConfig;

  @Test
  void shouldLoadUserByEmail() {
    // Given
    User user = new User();
    user.setEmail("test@test.com");
    user.setPassword("$2a$10$encoded");
    user.setRole(Role.GUEST);

    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

    UserDetailsService uds = securityConfig.userDetailsService(userRepository);

    // When
    UserDetails userDetails = uds.loadUserByUsername("test@test.com");

    // Then
    assertEquals("test@test.com", userDetails.getUsername());
    assertTrue(
        userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST")));
  }

  @Test
  void shouldThrowWhenUserNotFound() {
    when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

    UserDetailsService uds = securityConfig.userDetailsService(userRepository);

    assertThrows(UsernameNotFoundException.class, () -> uds.loadUserByUsername("missing@test.com"));
  }
}
