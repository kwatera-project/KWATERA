package io.github.kwatera_project.kwatera.auth_service.controller;

import io.github.kwatera_project.kwatera.auth_service.dto.UserProfileDto;
import io.github.kwatera_project.kwatera.auth_service.dto.UserProfileUpdateDto;
import io.github.kwatera_project.kwatera.auth_service.mapper.UserMapper;
import io.github.kwatera_project.kwatera.auth_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class UserProfileController {

  private final UserService userService;
  private final UserMapper userMapper;

  @GetMapping("/me")
  public ResponseEntity<UserProfileDto> getUserProfile(final Authentication authentication) {
    final var user = userService.getUserByEmail(authentication.getName());
    return ResponseEntity.ok(userMapper.toUserProfileDto(user));
  }

  @PutMapping("/me")
  public ResponseEntity<UserProfileDto> updateUserProfile(
      final Authentication authentication,
      @Valid @RequestBody final UserProfileUpdateDto request) {
    final var user =
        userService.updateProfile(
            authentication.getName(), request.firstName(), request.lastName());
    return ResponseEntity.ok(userMapper.toUserProfileDto(user));
  }
}
