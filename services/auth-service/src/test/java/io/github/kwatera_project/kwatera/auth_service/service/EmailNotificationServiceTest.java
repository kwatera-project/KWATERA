package io.github.kwatera_project.kwatera.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.mail.internet.MimeMessage;
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
  @Mock private MimeMessage mimeMessage;

  private EmailNotificationService emailNotificationService;

  @BeforeEach
  void setUp() {
    emailNotificationService =
        new EmailNotificationService(
            mailSender, templateEngine, "no-reply@kwatera.local", "test@kwatera.local");
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
