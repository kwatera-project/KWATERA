package io.github.kwatera_project.kwatera.auth_service.service;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtServiceTest {

  private final JwtService jwtService = new JwtService();

  @Test
  void shouldGenerateTokenAndExtractUsername() {
    UserDetails user =
        User.withUsername("test@test.com").password("password").authorities("ROLE_USER").build();

    String token = jwtService.generateToken(user);

    assertNotNull(token);
    assertFalse(token.isEmpty());

    String extractedUsername = jwtService.extractUsername(token);

    assertEquals("test@test.com", extractedUsername);
  }

  @Test
  void shouldValidateTokenForCorrectUser() {
    UserDetails user =
        User.withUsername("test@test.com").password("password").authorities("ROLE_USER").build();

    String token = jwtService.generateToken(user);

    boolean result = jwtService.isValid(token, user);

    assertTrue(result);
  }

  @Test
  void shouldInvalidateTokenForDifferentUser() {
    UserDetails user1 =
        User.withUsername("user1@test.com").password("password").authorities("ROLE_USER").build();

    UserDetails user2 =
        User.withUsername("user2@test.com").password("password").authorities("ROLE_USER").build();

    String token = jwtService.generateToken(user1);

    boolean result = jwtService.isValid(token, user2);

    assertFalse(result);
  }

  @Test
  void shouldContainExpirationTimeInFuture() {
    UserDetails user =
        User.withUsername("test@test.com").password("password").authorities("ROLE_USER").build();

    String token = jwtService.generateToken(user);

    Date expiration =
        Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor("supersecretkeysupersecretkey123456".getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration();

    assertTrue(expiration.after(new Date()));
  }
}
