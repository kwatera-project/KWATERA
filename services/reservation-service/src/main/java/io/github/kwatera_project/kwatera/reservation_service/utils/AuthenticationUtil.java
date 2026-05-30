package io.github.kwatera_project.kwatera.reservation_service.utils;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class AuthenticationUtil {

  private AuthenticationUtil() {}

  public record AuthContext(UUID userId, boolean isAdmin, boolean isOwner) {}

  public static AuthContext getAuthContext(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    boolean isAdmin =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    boolean isOwner =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));

    if (!isAdmin && !isOwner) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    Object details = authentication.getDetails();
    UUID userId = null;
    if (details instanceof String userIdString && !userIdString.isBlank()) {
      try {
        userId = UUID.fromString(userIdString);
      } catch (IllegalArgumentException _) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
      }
    } else {
      throw new ResponseStatusException(
          HttpStatus.UNAUTHORIZED, "Unauthorized: Token is incorrect");
    }

    return new AuthContext(userId, isAdmin, isOwner);
  }
}
