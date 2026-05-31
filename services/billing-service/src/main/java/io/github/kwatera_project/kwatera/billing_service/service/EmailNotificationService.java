package io.github.kwatera_project.kwatera.billing_service.service;

import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

  private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

  private final JavaMailSender mailSender;
  private final String fromAddress;
  private final String testRecipient;

  public EmailNotificationService(
      JavaMailSender mailSender,
      @Value("${kwatera.mail.from}") String fromAddress,
      @Value("${kwatera.mail.test-recipient}") String testRecipient) {
    this.mailSender = mailSender;
    this.fromAddress = fromAddress;
    this.testRecipient = testRecipient;
  }

  public void sendSettlementCreated(Settlement settlement, String recipientEmail) {
    String subject = "Settlement issued";
    String body =
        "A settlement was created for your reservation.\n\n"
            + "Settlement ID: "
            + settlement.getId()
            + "\nReservation ID: "
            + settlement.getReservationId()
            + "\nStatus: "
            + settlement.getStatus()
            + "\nTotal amount: "
            + settlement.getTotalAmount()
            + " PLN"
            + "\nBalance due: "
            + settlement.getBalanceDue()
            + " PLN";

    send(recipientEmail, subject, body, String.valueOf(settlement.getId()));
  }

  public void sendPaymentStatusChanged(
      Settlement settlement,
      SettlementStatus oldStatus,
      SettlementStatus newStatus,
      String recipientEmail) {
    String subject = "Payment status changed";
    String body =
        "Your settlement payment status changed.\n\n"
            + "Settlement ID: "
            + settlement.getId()
            + "\nReservation ID: "
            + settlement.getReservationId()
            + "\nPrevious status: "
            + oldStatus
            + "\nNew status: "
            + newStatus
            + "\nAmount paid: "
            + settlement.getAmountPaid()
            + " PLN"
            + "\nBalance due: "
            + settlement.getBalanceDue()
            + " PLN";

    send(recipientEmail, subject, body, String.valueOf(settlement.getId()));
  }

  private void send(String recipientEmail, String subject, String body, String referenceId) {
    String recipient = resolveRecipient(recipientEmail, subject, referenceId);
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(recipient);
    message.setSubject(subject);
    message.setText(body);

    try {
      mailSender.send(message);
      log.info("Sent email notification '{}' for {}", subject, referenceId);
    } catch (MailException e) {
      log.warn(
          "Failed to send email notification '{}' for {}: {}",
          subject,
          referenceId,
          e.getMessage());
    }
  }

  private String resolveRecipient(String recipientEmail, String subject, String referenceId) {
    if (recipientEmail != null && !recipientEmail.isBlank()) {
      return recipientEmail;
    }
    log.warn(
        "No recipient email available for notification '{}' for {}; using dev fallback {}",
        subject,
        referenceId,
        testRecipient);
    return testRecipient;
  }
}
