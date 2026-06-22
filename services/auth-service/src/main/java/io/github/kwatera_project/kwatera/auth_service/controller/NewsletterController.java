package io.github.kwatera_project.kwatera.auth_service.controller;

import io.github.kwatera_project.kwatera.auth_service.dto.SubscribeRequest;
import io.github.kwatera_project.kwatera.auth_service.model.NewsletterSubscriber;
import io.github.kwatera_project.kwatera.auth_service.repository.NewsletterSubscriberRepository;
import io.github.kwatera_project.kwatera.auth_service.service.EmailNotificationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
@CrossOrigin(origins = "${kwatera.urls.frontend-base}")
public class NewsletterController {

  private final NewsletterSubscriberRepository subscriberRepository;
  private final EmailNotificationService emailNotificationService;

  @Value("${kwatera.urls.frontend-base}")
  private String frontendBaseUrl;

  @PostMapping("/subscribe")
  public ResponseEntity<String> subscribe(@Valid @RequestBody SubscribeRequest request) {
    String email = request.getEmail().trim().toLowerCase();
    Optional<NewsletterSubscriber> existingOpt = subscriberRepository.findByEmail(email);

    if (existingOpt.isEmpty()) {
      String token = UUID.randomUUID().toString();
      NewsletterSubscriber subscriber = new NewsletterSubscriber();
      subscriber.setEmail(email);
      subscriber.setStatus("PENDING");
      subscriber.setToken(token);
      subscriber.setSubscribedAt(LocalDateTime.now());
      subscriberRepository.save(subscriber);
      emailNotificationService.sendNewsletterVerificationEmail(email, token);
    }

    return ResponseEntity.ok("Please check your email to confirm subscription.");
  }

  @GetMapping("/confirm")
  public ResponseEntity<Void> confirm(@RequestParam("token") String token) {
    Optional<NewsletterSubscriber> subscriberOpt = subscriberRepository.findByToken(token);
    if (subscriberOpt.isPresent()) {
      NewsletterSubscriber subscriber = subscriberOpt.get();
      if ("PENDING".equals(subscriber.getStatus())) {
        subscriber.setStatus("CONFIRMED");
        subscriber.setConfirmedAt(LocalDateTime.now());
        subscriberRepository.save(subscriber);
        emailNotificationService.sendNewsletterWelcomeEmail(subscriber.getEmail());
        emailNotificationService.sendWeeklyNewsletterEmail(subscriber.getEmail());
      }
    }
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(frontendBaseUrl + "/?newsletterConfirmed=true"))
        .build();
  }

  @GetMapping("/unsubscribe")
  public ResponseEntity<Void> unsubscribe(@RequestParam("token") String token) {
    Optional<NewsletterSubscriber> subscriberOpt = subscriberRepository.findByToken(token);
    subscriberOpt.ifPresent(subscriberRepository::delete);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(frontendBaseUrl + "/?newsletterUnsubscribed=true"))
        .build();
  }
}
