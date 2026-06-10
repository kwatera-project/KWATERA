package io.github.kwatera_project.kwatera.property_service.filter;

import io.github.kwatera_project.kwatera.property_service.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    try {
      if (jwtService.isValid(token)
          && SecurityContextHolder.getContext().getAuthentication() == null) {
        String userId = jwtService.extractUserId(token);
        List<String> roles = jwtService.extractRoles(token);

        List<SimpleGrantedAuthority> authorities =
            roles != null ? roles.stream().map(SimpleGrantedAuthority::new).toList() : List.of();

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(userId, null, authorities);

        authToken.setDetails(token);

        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    } catch (Exception e) {
      logger.error("Could not set user authentication in security context", e);
    }

    filterChain.doFilter(request, response);
  }
}
