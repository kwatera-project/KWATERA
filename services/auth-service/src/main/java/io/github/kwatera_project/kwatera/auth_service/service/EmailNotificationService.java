package io.github.kwatera_project.kwatera.auth_service.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.kwatera_project.kwatera.auth_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailNotificationService {

  private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final PropertyClient propertyClient;
  private final UserRepository userRepository;
  private final String fromAddress;
  private final String testRecipient;
  private final String publicGatewayUrl;
  private final String frontendBaseUrl;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public EmailNotificationService(
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      PropertyClient propertyClient,
      UserRepository userRepository,
      @Value("${kwatera.mail.from}") String fromAddress,
      @Value("${kwatera.mail.test-recipient}") String testRecipient,
      @Value("${kwatera.urls.public-gateway}") String publicGatewayUrl,
      @Value("${kwatera.urls.frontend-base}") String frontendBaseUrl) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.propertyClient = propertyClient;
    this.userRepository = userRepository;
    this.fromAddress = fromAddress;
    this.testRecipient = testRecipient;
    this.publicGatewayUrl = publicGatewayUrl;
    this.frontendBaseUrl = frontendBaseUrl;
  }

  public void sendThankYouEmail(String recipientEmail, String firstName) {
    String subject = "Thank you for registering!";
    Context context = new Context();
    context.setVariable("subject", subject);
    String sanitizedFirstName = firstName != null ? firstName.replaceAll("[\r\n]", "") : "User";
    context.setVariable("firstName", sanitizedFirstName);
    context.setVariable("loginLink", frontendBaseUrl + "/login");

    String htmlBody = templateEngine.process("thank-you-signup", context);
    send(recipientEmail, subject, htmlBody);
  }

  public void sendPasswordResetEmail(String recipientEmail, String resetUrl) {
    String subject = "Password Reset Request";
    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("resetUrl", resetUrl);

    String htmlBody = templateEngine.process("password-reset-template", context);
    send(recipientEmail, subject, htmlBody);
  }

  public void sendNewsletterWelcomeEmail(String recipientEmail) {
    String subject = "Welcome to KWATERA Newsletter!";
    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("homeLink", frontendBaseUrl);
    String htmlBody = templateEngine.process("welcome-newsletter-template", context);
    send(recipientEmail, subject, htmlBody);
  }

  public void sendNewsletterVerificationEmail(String recipientEmail, String token) {
    String subject = "Confirm your KWATERA subscription";
    Context context = new Context();
    context.setVariable("subject", subject);
    String confirmLink = publicGatewayUrl + "/api/newsletter/confirm?token=" + token;
    context.setVariable("confirmLink", confirmLink);
    String htmlBody = templateEngine.process("confirm-newsletter-template", context);
    send(recipientEmail, subject, htmlBody);
  }

  public void sendWeeklyNewsletterEmail(String recipientEmail) {
    String subject = "Your Weekly KWATERA Recommendations";
    Context context = new Context();
    context.setVariable("subject", subject);
    String greeting = "Subscriber";
    if (userRepository != null) {
      java.util.Optional<User> userOpt = userRepository.findByEmail(recipientEmail);
      if (userOpt.isPresent()
          && userOpt.get().getFirstName() != null
          && !userOpt.get().getFirstName().isBlank()) {
        greeting = userOpt.get().getFirstName();
      }
    }
    context.setVariable("greeting", greeting);
    List<Map<String, Object>> properties = propertyClient.getRandomProperties(3);
    List<Map<String, Object>> featuredItems = new ArrayList<>();
    for (Map<String, Object> prop : properties) {
      Map<String, Object> item = new HashMap<>();
      item.put("title", prop.get("title"));
      item.put("description", prop.get("description"));
      item.put("link", frontendBaseUrl + "/property/" + prop.get("id"));
      item.put("imageUrl", prop.get("imageUrl"));
      UUID propertyId = null;
      try {
        propertyId = UUID.fromString((String) prop.get("id"));
      } catch (IllegalArgumentException ignored) {
      }
      java.math.BigDecimal pricePerNight = null;
      if (propertyId != null) {
        List<Map<String, Object>> units = propertyClient.getPropertyUnits(propertyId);
        if (units != null) {
          for (Map<String, Object> unit : units) {
            Object priceObj = unit.get("pricePerNight");
            if (priceObj != null) {
              try {
                java.math.BigDecimal price = new java.math.BigDecimal(priceObj.toString());
                if (pricePerNight == null || price.compareTo(pricePerNight) < 0) {
                  pricePerNight = price;
                }
              } catch (NumberFormatException ignored) {
              }
            }
          }
        }
      }
      if (pricePerNight == null) {
        pricePerNight = new java.math.BigDecimal("250");
      }
      item.put("pricePerNight", pricePerNight);
      featuredItems.add(item);
    }
    context.setVariable("featuredItems", featuredItems);
    context.setVariable(
        "unsubscribeLink",
        publicGatewayUrl + "/api/newsletter/unsubscribe?email=" + recipientEmail);
    String htmlBody = templateEngine.process("weekly-newsletter-template", context);
    send(recipientEmail, subject, htmlBody);
  }

  private void send(String recipientEmail, String subject, String htmlBody) {
    String recipient = resolveRecipient(recipientEmail, subject);
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);

      mailSender.send(message);
      log.info("Sent email notification '{}'", subject);
    } catch (MailException | MessagingException e) {
      String safeErrorMessage =
          e.getMessage() != null ? e.getMessage().replaceAll("[\r\n]", "") : "Unknown error";
      log.warn("Failed to send email notification '{}': {}", subject, safeErrorMessage);
    }
  }

  private String resolveRecipient(String recipientEmail, String subject) {
    if (recipientEmail != null && !recipientEmail.isBlank()) {
      return recipientEmail;
    }
    log.warn(
        "No recipient email available for notification '{}'; using dev fallback {}",
        subject,
        testRecipient);
    return testRecipient;
  }
}
