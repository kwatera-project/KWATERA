package io.github.kwatera_project.kwatera.auth_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "newsletter_subscribers")
@Getter
@Setter
@NoArgsConstructor
public class NewsletterSubscriber {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  private String token;

  @Column(name = "subscribed_at", nullable = false)
  private LocalDateTime subscribedAt;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;
}
