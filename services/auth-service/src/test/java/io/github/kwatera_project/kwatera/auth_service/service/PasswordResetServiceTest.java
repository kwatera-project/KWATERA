package io.github.kwatera_project.kwatera.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.auth_service.model.PasswordResetToken;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.PasswordResetTokenRepository;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  @Mock private PasswordResetTokenRepository tokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private EmailNotificationService emailNotificationService;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private PasswordResetService passwordResetService;

  private User user;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = new User();
    user.setId(userId);
    user.setEmail("user@example.com");
    user.setPassword("old-password");
  }

  @Test
  void shouldInitiatePasswordResetSuccess() {
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    passwordResetService.initiatePasswordReset("user@example.com");

    verify(tokenRepository).deleteByUserId(userId);
    verify(tokenRepository).save(any(PasswordResetToken.class));
    verify(emailNotificationService).sendPasswordResetEmail(eq("user@example.com"), anyString());
  }

  @Test
  void shouldInitiatePasswordResetDoNotThrowOnEmailError() {
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    doThrow(new RuntimeException("SMTP failed"))
        .when(emailNotificationService)
        .sendPasswordResetEmail(eq("user@example.com"), anyString());

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> passwordResetService.initiatePasswordReset("user@example.com"));

    verify(tokenRepository).deleteByUserId(userId);
    verify(tokenRepository).save(any(PasswordResetToken.class));
  }

  @Test
  void shouldInitiatePasswordResetQuietlyWhenUserNotFound() {
    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

    passwordResetService.initiatePasswordReset("nonexistent@example.com");

    verify(tokenRepository, never()).deleteByUserId(any());
    verify(tokenRepository, never()).save(any());
    verify(emailNotificationService, never()).sendPasswordResetEmail(any(), any());
  }

  @Test
  void shouldResetPasswordSuccess() {
    PasswordResetToken token = new PasswordResetToken();
    token.setToken("valid-token");
    token.setUserId(userId);
    token.setExpiryDate(LocalDateTime.now().plusMinutes(15));

    when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");

    passwordResetService.resetPassword("valid-token", "new-password");

    assertThat(user.getPassword()).isEqualTo("encoded-new-password");
    verify(userRepository).save(user);
    verify(tokenRepository).deleteByUserId(userId);
  }

  @Test
  void shouldThrowWhenTokenNotFound() {
    when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> passwordResetService.resetPassword("invalid-token", "new-password"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Invalid token");

    verify(userRepository, never()).findById(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldDeleteTokenAndThrowWhenTokenExpired() {
    PasswordResetToken token = new PasswordResetToken();
    token.setToken("expired-token");
    token.setUserId(userId);
    token.setExpiryDate(LocalDateTime.now().minusMinutes(5));

    when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "new-password"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Token has expired");

    verify(tokenRepository).delete(token);
    verify(userRepository, never()).findById(any());
    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldThrowWhenUserNotFoundDuringReset() {
    PasswordResetToken token = new PasswordResetToken();
    token.setToken("valid-token");
    token.setUserId(userId);
    token.setExpiryDate(LocalDateTime.now().plusMinutes(15));

    when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", "new-password"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("User not found");

    verify(userRepository, never()).save(any());
    verify(tokenRepository, never()).deleteByUserId(any());
  }
}
