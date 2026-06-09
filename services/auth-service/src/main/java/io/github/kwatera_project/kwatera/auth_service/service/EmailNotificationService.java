package io.github.kwatera_project.kwatera.auth_service.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
  private final String fromAddress;
  private final String testRecipient;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public EmailNotificationService(
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      @Value("${kwatera.mail.from}") String fromAddress,
      @Value("${kwatera.mail.test-recipient}") String testRecipient) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.fromAddress = fromAddress;
    this.testRecipient = testRecipient;
  }

  public void sendThankYouEmail(String recipientEmail, String firstName) {
    String subject = "Thank you for registering!";
    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("firstName", firstName != null ? firstName : "User");

    String htmlBody = templateEngine.process("thank-you-signup", context);
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
      log.warn("Failed to send email notification '{}'", subject, e);
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
