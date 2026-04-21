package io.github.kwatera_project.kwatera.auth_service.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
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

  public String generateToken(UserDetails user) {
    return Jwts.builder()
        .subject(user.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1h
        .signWith(key)
        .compact();
  }

  public String extractUsername(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }

  public boolean isValid(String token, UserDetails user) {
    return extractUsername(token).equals(user.getUsername());
  }
}
