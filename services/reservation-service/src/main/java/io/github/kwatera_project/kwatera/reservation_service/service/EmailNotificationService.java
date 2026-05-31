package io.github.kwatera_project.kwatera.reservation_service.service;

import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
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

  public void sendReservationCreated(Reservation reservation, String recipientEmail) {
    String subject = "Reservation created";
    String body =
        "Your reservation was created.\n\n"
            + "Reservation ID: "
            + reservation.getId()
            + "\nUnit ID: "
            + reservation.getUnitId()
            + "\nDates: "
            + reservation.getStartDate()
            + " to "
            + reservation.getEndDate()
            + "\nStatus: "
            + reservation.getStatus()
            + "\nTotal price: "
            + reservation.getTotalPrice()
            + " PLN";

    send(recipientEmail, subject, body, String.valueOf(reservation.getId()));
  }

  public void sendReservationStatusChanged(
      Reservation reservation,
      ReservationStatus oldStatus,
      ReservationStatus newStatus,
      String recipientEmail) {
    String subject = "Reservation status changed";
    String body =
        "Your reservation status was updated.\n\n"
            + "Reservation ID: "
            + reservation.getId()
            + "\nPrevious status: "
            + oldStatus
            + "\nNew status: "
            + newStatus
            + "\nDates: "
            + reservation.getStartDate()
            + " to "
            + reservation.getEndDate();

    send(recipientEmail, subject, body, String.valueOf(reservation.getId()));
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
