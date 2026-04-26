package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "supersecretkeysupersecretkey123456";
  private JwtService jwtService;
  private SecretKey key;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(SECRET);
    key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void shouldThrowExceptionWhenSecretIsInvalid() {
    assertThrows(IllegalArgumentException.class, () -> new JwtService(null));
    assertThrows(IllegalArgumentException.class, () -> new JwtService("   "));
  }

  @Test
  void shouldExtractClaimsCorrectly() {
    String token =
        Jwts.builder()
            .subject("testuser")
            .claim("userId", "12345-uuid")
            .claim("role", List.of("ROLE_ADMIN"))
            .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
            .signWith(key)
            .compact();

    Claims claims = jwtService.extractAllClaims(token);
    assertNotNull(claims);
    assertEquals("testuser", claims.getSubject());

    assertEquals("testuser", jwtService.extractUsername(token));
    assertEquals("12345-uuid", jwtService.extractUserId(token));

    List<String> roles = jwtService.extractRoles(token);
    assertNotNull(roles);
    assertEquals(1, roles.size());
    assertEquals("ROLE_ADMIN", roles.get(0));
  }

  @Test
  void shouldReturnTrueForValidToken() {
    String token =
        Jwts.builder()
            .subject("testuser")
            .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
            .signWith(key)
            .compact();

    assertTrue(jwtService.isValid(token));
  }

  @Test
  void shouldReturnFalseForExpiredToken() {
    String token =
        Jwts.builder()
            .subject("testuser")
            .expiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // Past date
            .signWith(key)
            .compact();

    assertFalse(jwtService.isValid(token));
  }

  @Test
  void shouldReturnFalseForInvalidToken() {
    assertFalse(jwtService.isValid("invalid.token.string"));
  }
}
