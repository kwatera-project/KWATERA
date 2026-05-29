package io.github.kwatera_project.kwatera.billing_service.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @InjectMocks private JwtAuthFilter filter;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private static final String TEST_SECRET =
      "my_super_secret_key_which_is_long_enough_for_hmac_sha256";

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(filter, "secret", TEST_SECRET);
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSkipWhenNoAuthorizationHeader() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldSkipWhenHeaderDoesNotStartWithBearer() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic abc");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldReturnUnauthorizedOnInvalidToken() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");

    filter.doFilterInternal(request, response, filterChain);

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verifyNoInteractions(filterChain);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldAuthenticateUser_whenValidTokenWithRolesProvided() throws Exception {
    String userId = UUID.randomUUID().toString();
    Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    String token =
        Jwts.builder()
            .setSubject(userId)
            .claim("role", List.of("ROLE_ADMIN", "ROLE_OWNER"))
            .signWith(key)
            .compact();

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    assertTrue(
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  @Test
  void shouldAuthenticateUserWithoutRoles_whenTokenHasNoRole() throws Exception {
    String userId = UUID.randomUUID().toString();
    Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    String token = Jwts.builder().setSubject(userId).signWith(key).compact();

    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals(userId, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());
  }
}
