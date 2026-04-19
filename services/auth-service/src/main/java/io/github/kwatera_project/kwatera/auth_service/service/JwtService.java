package io.github.kwatera_project.kwatera.auth_service.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final String SECRET = "supersecretkeysupersecretkey123456";

  private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

  public String generateToken(UserDetails user) {
    return Jwts.builder()
        .subject(user.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1h
        .signWith(key)
        .compact();
  }

  public String extractUsername(String token) {
    return Jwts.parser()
        .verifyWith((SecretKey) key)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public boolean isValid(String token, UserDetails user) {
    return extractUsername(token).equals(user.getUsername());
  }
}
