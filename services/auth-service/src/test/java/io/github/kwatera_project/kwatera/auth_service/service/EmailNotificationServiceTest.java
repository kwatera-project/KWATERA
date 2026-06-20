package io.github.kwatera_project.kwatera.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.auth_service.client.PropertyClient;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
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
  @Mock private MimeMessage mimeMessage;

  private EmailNotificationService emailNotificationService;

  @BeforeEach
  void setUp() {
    emailNotificationService =
        new EmailNotificationService(
            mailSender, templateEngine, propertyClient, "no-reply@kwatera.local", "test@kwatera.local");
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
    prop.put("id", "12345");
    prop.put("title", "Luxury Villa");
    prop.put("description", "A beautiful villa");
    when(propertyClient.getRandomProperties(3)).thenReturn(java.util.Collections.singletonList(prop));
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
    assertThat(item.get("link")).isEqualTo("http://localhost:5173/property/12345");
    assertThat(context.getVariable("unsubscribeLink")).isEqualTo("http://localhost:8090/api/newsletter/unsubscribe?email=subscriber@example.com");
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
