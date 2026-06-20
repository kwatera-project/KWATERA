package io.github.kwatera_project.kwatera.auth_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(name = "user_id", nullable = false)
  private java.util.UUID userId;

  @Column(name = "expiry_date", nullable = false)
  private LocalDateTime expiryDate;
}
