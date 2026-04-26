package io.github.kwatera_project.kwatera.reservation_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class JwtService {

  private final SecretKey key;

  public JwtService(@Value("${jwt.secret}") String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("JWT secret cannot be null or empty");
    }
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  public String extractUserId(String token) {
    return extractAllClaims(token).get("userId", String.class);
  }

  @SuppressWarnings("unchecked")
  public List<String> extractRoles(String token) {
    return extractAllClaims(token).get("role", List.class);
  }

  private boolean isTokenExpired(String token) {
    return extractAllClaims(token).getExpiration().before(new Date());
  }

  public boolean isValid(String token) {
    try {
      return !isTokenExpired(token);
    } catch (Exception e) {
      return false;
    }
  }
}
