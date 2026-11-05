package io.github.kwatera_project.kwatera.ai_service.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.kwatera_project.kwatera.ai_service.error.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class InternalTokenFilter extends OncePerRequestFilter {
  private final String token;
  private final ObjectMapper mapper;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed JSON mapper")
  public InternalTokenFilter(InternalSecurityProperties properties, ObjectMapper mapper) {
    this.token = properties.internalToken();
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if ("GET".equals(request.getMethod())
        && (path.equals("/actuator/health/liveness")
            || path.equals("/actuator/health/readiness")
            || path.equals("/v3/api-docs")
            || path.equals("/v3/api-docs.yaml")
            || path.startsWith("/v3/api-docs/")
            || path.equals("/swagger-ui.html")
            || path.startsWith("/swagger-ui/"))) {
      chain.doFilter(request, response);
      return;
    }
    String supplied = request.getHeader("X-Internal-Token");
    if (supplied == null
        || !MessageDigest.isEqual(
            token.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
      response.setStatus(403);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      mapper.writeValue(
          response.getOutputStream(),
          ApiError.of("ACCESS_DENIED", "A valid internal service token is required.", 403));
      return;
    }
    chain.doFilter(request, response);
  }
}
