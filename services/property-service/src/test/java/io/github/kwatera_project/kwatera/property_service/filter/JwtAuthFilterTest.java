package io.github.kwatera_project.kwatera.property_service.filter;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.property_service.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthFilterTest {

  @Mock private JwtService jwtService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthFilter jwtAuthFilter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldPassThroughWhenNoAuthHeader() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn(null);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldPassThroughWhenHeaderDoesNotStartWithBearer() throws ServletException, IOException {
    when(request.getHeader("Authorization")).thenReturn("Basic abc");

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldSetAuthenticationWhenTokenIsValid() throws Exception {
    String token = "valid.jwt.token";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isValid(token)).thenReturn(true);
    when(jwtService.extractUsername(token)).thenReturn("john");
    when(jwtService.extractUserId(token)).thenReturn("123");
    when(jwtService.extractRoles(token)).thenReturn(List.of("ROLE_USER"));

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    assertNotNull(auth);
    assertEquals("123", auth.getPrincipal());
    assertEquals("valid.jwt.token", auth.getDetails());
    assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotSetAuthenticationWhenJwtInvalid() throws Exception {
    String token = "invalid.jwt";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isValid(token)).thenReturn(false);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldHandleExceptionGracefully() throws Exception {
    String token = "broken.jwt";

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isValid(token)).thenThrow(new RuntimeException("boom"));

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
