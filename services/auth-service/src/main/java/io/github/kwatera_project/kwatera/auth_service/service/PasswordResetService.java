package io.github.kwatera_project.kwatera.auth_service.service;

import io.github.kwatera_project.kwatera.auth_service.model.PasswordResetToken;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.PasswordResetTokenRepository;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final PasswordResetTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final EmailNotificationService emailNotificationService;
  private final PasswordEncoder passwordEncoder;

  @Value("${kwatera.frontend.url:http://localhost:5173}")
  private String frontendUrl;

  @Transactional
  public void initiatePasswordReset(String email) {
    Optional<User> userOpt = userRepository.findByEmail(email);
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      tokenRepository.deleteByUserId(user.getId());

      String tokenVal = UUID.randomUUID().toString();
      PasswordResetToken resetToken = new PasswordResetToken();
      resetToken.setToken(tokenVal);
      resetToken.setUserId(user.getId());
      resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
      tokenRepository.save(resetToken);

      String resetUrl = frontendUrl + "/reset-password?token=" + tokenVal;
      try {
        emailNotificationService.sendPasswordResetEmail(user.getEmail(), resetUrl);
      } catch (Exception e) {
        log.error(
            "Failed to send password reset email to user {}: {}", user.getId(), e.getMessage());
      }
    } else {
      log.info("Password reset requested for non-existent email: {}", email);
    }
  }

  @Transactional
  public void resetPassword(String token, String newPassword) {
    PasswordResetToken resetToken =
        tokenRepository
            .findByToken(token)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

    if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
      tokenRepository.delete(resetToken);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token has expired");
    }

    User user =
        userRepository
            .findById(resetToken.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    tokenRepository.deleteByUserId(user.getId());
  }
}
