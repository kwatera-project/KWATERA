package io.github.kwatera_project.kwatera.auth_service.filter;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.auth_service.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {
  @Mock private JwtService jwtService;
  @Mock private UserDetailsService userDetailsService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private JwtAuthFilter filter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    filter = new JwtAuthFilter(jwtService, userDetailsService);
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSkipFilterWhenNoAuthHeader() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldSkipFilterWhenHeaderDoesNotStartWithBearer() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic abc123");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldAuthenticateUserWhenTokenIsValid() throws Exception {
    String token = "valid-token";
    String username = "test@test.com";

    UserDetails user =
        User.withUsername(username).password("pass").authorities("ROLE_USER").build();

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.extractUsername(token)).thenReturn(username);
    when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
    when(jwtService.isValid(token, user)).thenReturn(true);

    filter.doFilterInternal(request, response, filterChain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    assertNotNull(auth);
    assertEquals(username, ((UserDetails) auth.getPrincipal()).getUsername());
    assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotAuthenticateWhenTokenIsInvalid() throws Exception {
    String token = "invalid-token";
    String username = "test@test.com";

    UserDetails user =
        User.withUsername(username).password("pass").authorities("ROLE_USER").build();

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.extractUsername(token)).thenReturn(username);
    when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
    when(jwtService.isValid(token, user)).thenReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}
