package io.github.kwatera_project.kwatera.auth_service.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {

    return ResponseEntity.status(ex.getStatusCode())
        .body(
            Map.of(
                "error",
                ex.getReason() != null ? ex.getReason() : "An error occurred",
                "status",
                ex.getStatusCode().value()));
  }

  @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
  public ResponseEntity<Map<String, Object>> handleAuthenticationException(
      AuthenticationException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", "Invalid credentials", "status", HttpStatus.UNAUTHORIZED.value()));
  }
}
