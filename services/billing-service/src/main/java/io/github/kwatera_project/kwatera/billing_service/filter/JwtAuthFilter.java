package io.github.kwatera_project.kwatera.billing_service.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  @Value("${jwt.secret}")
  private String secret;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String header = request.getHeader("Authorization");

    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = header.substring(7);

    try {
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
              .build()
              .parseClaimsJws(token)
              .getBody();

      String username = claims.getSubject();
      String userId = claims.get("userId", String.class);
      if (userId == null || userId.isBlank()) {
        userId = username;
      }

      List<?> rolesList = claims.get("role", List.class);
      List<SimpleGrantedAuthority> authorities =
          rolesList != null
              ? rolesList.stream().map(role -> new SimpleGrantedAuthority(role.toString())).toList()
              : List.of();

      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(username, null, authorities);
      auth.setDetails(userId);

      SecurityContextHolder.getContext().setAuthentication(auth);

    } catch (Exception _) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    filterChain.doFilter(request, response);
  }
}
