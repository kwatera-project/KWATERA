package io.github.kwatera_project.kwatera.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.auth_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.auth_service.model.User;
import io.github.kwatera_project.kwatera.auth_service.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

  @Mock private JavaMailSender mailSender;
  @Mock private TemplateEngine templateEngine;
  @Mock private PropertyClient propertyClient;
  @Mock private UserRepository userRepository;
  @Mock private MimeMessage mimeMessage;

  private EmailNotificationService emailNotificationService;

  @BeforeEach
  void setUp() {
    emailNotificationService =
        new EmailNotificationService(
            mailSender,
            templateEngine,
            propertyClient,
            userRepository,
            "no-reply@kwatera.local",
            "test@kwatera.local",
            "http://localhost:8090",
            "http://localhost:5173");
  }

  @Test
  void shouldSendThankYouEmail() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("thank-you-signup"), any(Context.class)))
        .thenReturn("<html>Welcome!</html>");

    emailNotificationService.sendThankYouEmail("user@example.com", "Alice");

    verify(mailSender).send(mimeMessage);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("thank-you-signup"), contextCaptor.capture());

    Context context = contextCaptor.getValue();
    assertThat(context.getVariable("firstName")).isEqualTo("Alice");
    assertThat(context.getVariable("subject")).isEqualTo("Thank you for registering!");
  }

  @Test
  void shouldSendNewsletterWelcomeEmail() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("welcome-newsletter-template"), any(Context.class)))
        .thenReturn("<html>Welcome!</html>");

    emailNotificationService.sendNewsletterWelcomeEmail("subscriber@example.com");

    verify(mailSender).send(mimeMessage);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("welcome-newsletter-template"), contextCaptor.capture());

    Context context = contextCaptor.getValue();
    assertThat(context.getVariable("subject")).isEqualTo("Welcome to KWATERA Newsletter!");
  }

  @Test
  void shouldSendNewsletterVerificationEmail() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("confirm-newsletter-template"), any(Context.class)))
        .thenReturn("<html>Confirm!</html>");

    emailNotificationService.sendNewsletterVerificationEmail(
        "subscriber@example.com", "test-token");

    verify(mailSender).send(mimeMessage);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("confirm-newsletter-template"), contextCaptor.capture());

    Context context = contextCaptor.getValue();
    assertThat(context.getVariable("subject")).isEqualTo("Confirm your KWATERA subscription");
    assertThat(context.getVariable("confirmLink"))
        .isEqualTo("http://localhost:8090/api/newsletter/confirm?token=test-token");
  }

  @Test
  void shouldSendWeeklyNewsletterEmail() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    Map<String, Object> prop = new java.util.HashMap<>();
    prop.put("id", "00000000-0000-0000-0000-000000000001");
    prop.put("title", "Luxury Villa");
    prop.put("description", "A beautiful villa");
    prop.put("imageUrl", "http://image.url");
    when(propertyClient.getRandomProperties(3))
        .thenReturn(java.util.Collections.singletonList(prop));
    when(userRepository.findByEmail("subscriber@example.com")).thenReturn(Optional.empty());

    Map<String, Object> unit = new java.util.HashMap<>();
    unit.put("pricePerNight", 350.0);
    when(propertyClient.getPropertyUnits(UUID.fromString("00000000-0000-0000-0000-000000000001")))
        .thenReturn(java.util.Collections.singletonList(unit));

    when(templateEngine.process(eq("weekly-newsletter-template"), any(Context.class)))
        .thenReturn("<html>Weekly Newsletter!</html>");

    emailNotificationService.sendWeeklyNewsletterEmail("subscriber@example.com");

    verify(mailSender).send(mimeMessage);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("weekly-newsletter-template"), contextCaptor.capture());

    Context context = contextCaptor.getValue();
    assertThat(context.getVariable("subject")).isEqualTo("Your Weekly KWATERA Recommendations");
    assertThat(context.getVariable("greeting")).isEqualTo("Subscriber");
    List<?> items = (List<?>) context.getVariable("featuredItems");
    assertThat(items).hasSize(1);
    Map<?, ?> item = (Map<?, ?>) items.get(0);
    assertThat(item.get("title")).isEqualTo("Luxury Villa");
    assertThat(item.get("description")).isEqualTo("A beautiful villa");
    assertThat(item.get("link"))
        .isEqualTo("http://localhost:5173/property/00000000-0000-0000-0000-000000000001");
    assertThat(item.get("imageUrl")).isEqualTo("http://image.url");
    assertThat(item.get("pricePerNight")).isEqualTo(new BigDecimal("350.0"));
    assertThat(context.getVariable("unsubscribeLink"))
        .isEqualTo("http://localhost:8090/api/newsletter/unsubscribe?email=subscriber@example.com");
  }

  @Test
  void shouldSendWeeklyNewsletterEmailWithUserFirstName() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    Map<String, Object> prop = new java.util.HashMap<>();
    prop.put("id", "00000000-0000-0000-0000-000000000001");
    prop.put("title", "Luxury Villa");
    prop.put("description", "A beautiful villa");
    prop.put("imageUrl", "http://image.url");
    when(propertyClient.getRandomProperties(3))
        .thenReturn(java.util.Collections.singletonList(prop));

    User user = new User();
    user.setFirstName("Alice");
    when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

    when(propertyClient.getPropertyUnits(UUID.fromString("00000000-0000-0000-0000-000000000001")))
        .thenReturn(java.util.Collections.emptyList());

    when(templateEngine.process(eq("weekly-newsletter-template"), any(Context.class)))
        .thenReturn("<html>Weekly Newsletter!</html>");

    emailNotificationService.sendWeeklyNewsletterEmail("alice@example.com");

    verify(mailSender).send(mimeMessage);

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("weekly-newsletter-template"), contextCaptor.capture());

    Context context = contextCaptor.getValue();
    assertThat(context.getVariable("subject")).isEqualTo("Your Weekly KWATERA Recommendations");
    assertThat(context.getVariable("greeting")).isEqualTo("Alice");
    List<?> items = (List<?>) context.getVariable("featuredItems");
    assertThat(items).hasSize(1);
    Map<?, ?> item = (Map<?, ?>) items.get(0);
    assertThat(item.get("pricePerNight")).isEqualTo(new BigDecimal("250"));
  }

  @Test
  void shouldSendWeeklyNewsletterWithFallbackWhenUserFirstNameIsBlank() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    Map<String, Object> prop = new java.util.HashMap<>();
    prop.put("id", "00000000-0000-0000-0000-000000000001");
    prop.put("title", "Luxury Villa");
    prop.put("description", "A beautiful villa");
    when(propertyClient.getRandomProperties(3))
        .thenReturn(java.util.Collections.singletonList(prop));

    User user = new User();
    user.setFirstName("   ");
    when(userRepository.findByEmail("blank@example.com")).thenReturn(Optional.of(user));

    when(templateEngine.process(eq("weekly-newsletter-template"), any(Context.class)))
        .thenReturn("<html>Weekly Newsletter!</html>");

    emailNotificationService.sendWeeklyNewsletterEmail("blank@example.com");

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("weekly-newsletter-template"), contextCaptor.capture());
    assertThat(contextCaptor.getValue().getVariable("greeting")).isEqualTo("Subscriber");
  }

  @Test
  void shouldSendWeeklyNewsletterWithFallbackWhenRepositoryIsNull() {
    EmailNotificationService serviceWithNullRepo =
        new EmailNotificationService(
            mailSender,
            templateEngine,
            propertyClient,
            null,
            "no-reply@kwatera.local",
            "test@kwatera.local",
            "http://localhost:8090",
            "http://localhost:5173");

    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    Map<String, Object> prop = new java.util.HashMap<>();
    prop.put("id", "00000000-0000-0000-0000-000000000001");
    prop.put("title", "Luxury Villa");
    prop.put("description", "A beautiful villa");
    when(propertyClient.getRandomProperties(3))
        .thenReturn(java.util.Collections.singletonList(prop));

    when(templateEngine.process(eq("weekly-newsletter-template"), any(Context.class)))
        .thenReturn("<html>Weekly Newsletter!</html>");

    serviceWithNullRepo.sendWeeklyNewsletterEmail("norepo@example.com");

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("weekly-newsletter-template"), contextCaptor.capture());
    assertThat(contextCaptor.getValue().getVariable("greeting")).isEqualTo("Subscriber");
  }

  @Test
  void shouldHandleInvalidPropertyIdAndNullUnits() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    Map<String, Object> prop1 = new java.util.HashMap<>();
    prop1.put("id", "invalid-uuid");
    prop1.put("title", "Invalid Villa");

    Map<String, Object> prop2 = new java.util.HashMap<>();
    prop2.put("id", "00000000-0000-0000-0000-000000000002");
    prop2.put("title", "Null Units Villa");

    when(propertyClient.getRandomProperties(3)).thenReturn(java.util.Arrays.asList(prop1, prop2));
    when(userRepository.findByEmail("any@example.com")).thenReturn(Optional.empty());

    when(propertyClient.getPropertyUnits(UUID.fromString("00000000-0000-0000-0000-000000000002")))
        .thenReturn(null);

    when(templateEngine.process(eq("weekly-newsletter-template"), any(Context.class)))
        .thenReturn("<html>Weekly Newsletter!</html>");

    emailNotificationService.sendWeeklyNewsletterEmail("any@example.com");

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("weekly-newsletter-template"), contextCaptor.capture());
    List<?> items = (List<?>) contextCaptor.getValue().getVariable("featuredItems");
    assertThat(items).hasSize(2);

    Map<?, ?> item1 = (Map<?, ?>) items.get(0);
    assertThat(item1.get("pricePerNight")).isEqualTo(new BigDecimal("250"));

    Map<?, ?> item2 = (Map<?, ?>) items.get(1);
    assertThat(item2.get("pricePerNight")).isEqualTo(new BigDecimal("250"));
  }

  @Test
  void shouldHandleMultipleUnitsSelectingMinPriceAndInvalidPrices() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    Map<String, Object> prop = new java.util.HashMap<>();
    prop.put("id", "00000000-0000-0000-0000-000000000001");
    prop.put("title", "Luxury Villa");
    when(propertyClient.getRandomProperties(3))
        .thenReturn(java.util.Collections.singletonList(prop));
    when(userRepository.findByEmail("any@example.com")).thenReturn(Optional.empty());

    Map<String, Object> unit1 = new java.util.HashMap<>();
    unit1.put("pricePerNight", "invalid-price-format");

    Map<String, Object> unit2 = new java.util.HashMap<>();
    unit2.put("pricePerNight", 450.0);

    Map<String, Object> unit3 = new java.util.HashMap<>();
    unit3.put("pricePerNight", 150.0);

    Map<String, Object> unit4 = new java.util.HashMap<>();
    unit4.put("pricePerNight", null);

    when(propertyClient.getPropertyUnits(UUID.fromString("00000000-0000-0000-0000-000000000001")))
        .thenReturn(java.util.Arrays.asList(unit1, unit2, unit3, unit4));

    when(templateEngine.process(eq("weekly-newsletter-template"), any(Context.class)))
        .thenReturn("<html>Weekly Newsletter!</html>");

    emailNotificationService.sendWeeklyNewsletterEmail("any@example.com");

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq("weekly-newsletter-template"), contextCaptor.capture());
    List<?> items = (List<?>) contextCaptor.getValue().getVariable("featuredItems");
    Map<?, ?> item = (Map<?, ?>) items.get(0);
    assertThat(item.get("pricePerNight")).isEqualTo(new BigDecimal("150.0"));
  }

  @Test
  void shouldNotThrowWhenMailSendingFails() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("thank-you-signup"), any(Context.class)))
        .thenReturn("<html>Welcome!</html>");
    doThrow(new org.springframework.mail.MailSendException("SMTP error"))
        .when(mailSender)
        .send(any(MimeMessage.class));

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> emailNotificationService.sendThankYouEmail("user@example.com", "Alice"));
  }

  @Test
  void shouldUseFallbackRecipientWhenRecipientIsBlank() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("thank-you-signup"), any(Context.class)))
        .thenReturn("<html>Welcome!</html>");

    emailNotificationService.sendThankYouEmail(" ", "Alice");

    verify(mailSender).send(mimeMessage);
  }

  @Test
  void shouldUseFallbackRecipientWhenRecipientIsNull() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("thank-you-signup"), any(Context.class)))
        .thenReturn("<html>Welcome!</html>");

    emailNotificationService.sendThankYouEmail(null, null);

    verify(mailSender).send(mimeMessage);
  }

  @Test
  void shouldNotThrowWhenMailSendingFailsWithNullMessage() {
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(eq("thank-you-signup"), any(Context.class)))
        .thenReturn("<html>Welcome!</html>");
    doThrow(new org.springframework.mail.MailSendException((String) null))
        .when(mailSender)
        .send(any(MimeMessage.class));

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> emailNotificationService.sendThankYouEmail("user@example.com", "Alice"));
  }
}
