package io.github.kwatera_project.kwatera.reservation_service.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.reservation_service.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @Mock private JwtService jwtService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthFilter jwtAuthFilter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldContinueChainWhenAuthHeaderIsMissing() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldContinueChainWhenAuthHeaderIsInvalid() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic token");

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldSetAuthenticationWhenTokenIsValid() throws Exception {
    String token = "valid-token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isValid(token)).thenReturn(true);
    when(jwtService.extractUsername(token)).thenReturn("user@example.com");
    when(jwtService.extractUserId(token)).thenReturn("12345");
    when(jwtService.extractRoles(token)).thenReturn(List.of("ROLE_ADMIN"));

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(auth);
    assertEquals("user@example.com", auth.getName());
    assertEquals("12345", auth.getDetails());
    assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotSetAuthenticationWhenTokenIsInvalid() throws Exception {
    String token = "invalid-token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isValid(token)).thenReturn(false);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotSetAuthenticationWhenExceptionIsThrown() throws Exception {
    String token = "exception-token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isValid(token)).thenThrow(new RuntimeException("Parsing error"));

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldSetEmptyAuthoritiesWhenRolesAreNull() throws Exception {
    String token = "no-roles-token";
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    when(jwtService.isValid(token)).thenReturn(true);
    when(jwtService.extractUsername(token)).thenReturn("user@example.com");
    when(jwtService.extractUserId(token)).thenReturn("12345");
    when(jwtService.extractRoles(token)).thenReturn(null);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(auth);
    assertTrue(auth.getAuthorities().isEmpty());
    verify(filterChain).doFilter(request, response);
  }
}
