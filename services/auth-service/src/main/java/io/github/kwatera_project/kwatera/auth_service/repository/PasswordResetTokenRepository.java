package io.github.kwatera_project.kwatera.auth_service.repository;

import io.github.kwatera_project.kwatera.auth_service.model.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  Optional<PasswordResetToken> findByToken(String token);

  void deleteByUserId(UUID userId);
}
