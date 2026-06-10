package io.github.kwatera_project.kwatera.auth_service.controller;

import io.github.kwatera_project.kwatera.auth_service.dto.UserProfileDto;
import io.github.kwatera_project.kwatera.auth_service.dto.UserProfileUpdateDto;
import io.github.kwatera_project.kwatera.auth_service.mapper.UserMapper;
import io.github.kwatera_project.kwatera.auth_service.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserService userService;
  private final UserMapper userMapper;

  @Value("${kwatera.security.internal-token:kwatera-internal-secret-token}")
  private String expectedInternalToken;

  @GetMapping("/me")
  public ResponseEntity<UserProfileDto> getUserProfile(final Authentication authentication) {
    final var user = userService.getUserByEmail(authentication.getName());
    return ResponseEntity.ok(userMapper.toUserProfileDto(user));
  }

  @PutMapping("/me")
  public ResponseEntity<UserProfileDto> updateUserProfile(
      final Authentication authentication, @Valid @RequestBody final UserProfileUpdateDto request) {
    final var user =
        userService.updateProfile(
            authentication.getName(), request.firstName(), request.lastName());
    return ResponseEntity.ok(userMapper.toUserProfileDto(user));
  }

  @GetMapping("/internal/{userId}")
  public ResponseEntity<UserProfileDto> getUserProfileInternal(
      @PathVariable("userId") UUID userId,
      @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
    if (internalToken == null || !internalToken.equals(expectedInternalToken)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Access denied: Invalid internal token");
    }
    final var user = userService.getUserById(userId);
    return ResponseEntity.ok(userMapper.toUserProfileDto(user));
  }

  @GetMapping("/internal/by-email/{email}")
  public ResponseEntity<UserProfileDto> getUserProfileByEmailInternal(
      @PathVariable("email") String email,
      @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {
    if (internalToken == null || !internalToken.equals(expectedInternalToken)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Access denied: Invalid internal token");
    }
    final var user = userService.getUserByEmail(email);
    return ResponseEntity.ok(userMapper.toUserProfileDto(user));
  }
}
