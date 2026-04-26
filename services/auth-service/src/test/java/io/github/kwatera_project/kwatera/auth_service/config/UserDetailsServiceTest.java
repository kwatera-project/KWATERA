package io.github.kwatera_project.kwatera.auth_service.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.auth_service.model.Role;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceTest {

  @Mock UserRepository userRepository;

  private UserDetailsService userDetailsService(UserRepository repo) {
    return email ->
        repo.findByEmail(email)
            .map(
                user ->
                    org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  @Test
  void shouldLoadUserByEmail() {
    User user = new User();
    user.setEmail("test@test.com");
    user.setPassword("encoded");
    user.setRole(Role.GUEST);

    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

    UserDetailsService uds = userDetailsService(userRepository);

    UserDetails userDetails = uds.loadUserByUsername("test@test.com");

    assertEquals("test@test.com", userDetails.getUsername());
    assertEquals("encoded", userDetails.getPassword());

    assertTrue(
        userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST")));

    verify(userRepository).findByEmail("test@test.com");
  }

  @Test
  void shouldThrowExceptionWhenUserNotFound() {
    when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

    UserDetailsService uds = userDetailsService(userRepository);

    assertThrows(UsernameNotFoundException.class, () -> uds.loadUserByUsername("missing@test.com"));
  }

  @Test
  void shouldCallRepositoryOnce() {
    User user = new User();
    user.setEmail("test@test.com");
    user.setPassword("encoded");
    user.setRole(Role.GUEST);

    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

    UserDetailsService uds = userDetailsService(userRepository);

    uds.loadUserByUsername("test@test.com");

    verify(userRepository, times(1)).findByEmail("test@test.com");
  }
}
