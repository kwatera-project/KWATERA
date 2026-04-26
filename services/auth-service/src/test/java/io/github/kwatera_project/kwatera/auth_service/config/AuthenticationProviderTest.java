package io.github.kwatera_project.kwatera.auth_service.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class AuthenticationProviderTest {

  @Mock UserDetailsService userDetailsService;

  AuthenticationProvider authenticationProvider;

  @BeforeEach
  void setUp() {
    authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
  }

  @Test
  void shouldAuthenticateValidUser() {
    // Given
    UserDetails user =
        User.builder().username("user@test.com").password("{noop}password").roles("GUEST").build();

    when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(user);

    Authentication token = new UsernamePasswordAuthenticationToken("user@test.com", "password");

    // When
    Authentication result = authenticationProvider.authenticate(token);

    // Then
    assertTrue(result.isAuthenticated());
  }
}
