package io.github.kwatera_project.kwatera.reservation_service.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailNotificationService {

  private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final RestTemplate restTemplate;
  private final String fromAddress;
  private final String testRecipient;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public EmailNotificationService(
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      RestTemplate restTemplate,
      @Value("${kwatera.mail.from}") String fromAddress,
      @Value("${kwatera.mail.test-recipient}") String testRecipient) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.restTemplate = restTemplate;
    this.fromAddress = fromAddress;
    this.testRecipient = testRecipient;
  }

  public void sendReservationCreated(Reservation reservation, String recipientEmail) {
    String subject = "Reservation created";
    String propertyName = fetchPropertyName(reservation.getUnitId());
    long numberOfNights =
        ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate());

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("title", "Reservation Created");
    context.setVariable(
        "message",
        "Your reservation has been successfully created. Here are the details of your upcoming stay:");
    context.setVariable("reservation", reservation);
    context.setVariable("propertyName", propertyName);
    context.setVariable("numberOfNights", numberOfNights);
    context.setVariable("statusLabel", reservation.getStatus().name());
    context.setVariable("statusStyle", getStatusStyle(reservation.getStatus()));
    context.setVariable("oldStatus", null);
    context.setVariable(
        "formattedPrice",
        formatPrice(
            reservation.getTotalPrice(),
            reservation.getPaymentCurrency(),
            reservation.getPaymentExchangeRate()));

    String htmlBody = templateEngine.process("reservation-confirmation", context);
    send(recipientEmail, subject, htmlBody, String.valueOf(reservation.getId()));
  }

  public void sendReservationStatusChanged(
      Reservation reservation,
      ReservationStatus oldStatus,
      ReservationStatus newStatus,
      String recipientEmail) {
    String subject = "Reservation status changed";
    String propertyName = fetchPropertyName(reservation.getUnitId());
    long numberOfNights =
        ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate());

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("title", "Reservation Status Updated");
    context.setVariable(
        "message",
        "Your reservation status was updated. Please review the details of the change below:");
    context.setVariable("reservation", reservation);
    context.setVariable("propertyName", propertyName);
    context.setVariable("numberOfNights", numberOfNights);
    context.setVariable("statusLabel", newStatus.name());
    context.setVariable("statusStyle", getStatusStyle(newStatus));
    context.setVariable("oldStatus", oldStatus.name());
    context.setVariable("newStatus", newStatus.name());
    context.setVariable(
        "formattedPrice",
        formatPrice(
            reservation.getTotalPrice(),
            reservation.getPaymentCurrency(),
            reservation.getPaymentExchangeRate()));

    String htmlBody = templateEngine.process("reservation-confirmation", context);
    send(recipientEmail, subject, htmlBody, String.valueOf(reservation.getId()));
  }

  public void sendOwnerReservationCreated(Reservation reservation) {
    String subject = "New booking Alert";
    String ownerEmail = fetchOwnerEmail(reservation.getUnitId());
    String propertyName = fetchPropertyName(reservation.getUnitId());
    long numberOfNights =
        ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate());

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("reservation", reservation);
    context.setVariable("propertyName", propertyName);
    context.setVariable("numberOfNights", numberOfNights);
    context.setVariable(
        "formattedPrice",
        formatPrice(
            reservation.getTotalPrice(),
            reservation.getPaymentCurrency(),
            reservation.getPaymentExchangeRate()));

    String htmlBody = templateEngine.process("owner-reservation-created", context);
    send(ownerEmail, subject, htmlBody, String.valueOf(reservation.getId()));
  }

  public void sendOwnerReservationCancelled(Reservation reservation) {
    String subject = "Reservation Cancelled Alert";
    String ownerEmail = fetchOwnerEmail(reservation.getUnitId());
    String propertyName = fetchPropertyName(reservation.getUnitId());
    long numberOfNights =
        ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate());

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("reservation", reservation);
    context.setVariable("propertyName", propertyName);
    context.setVariable("numberOfNights", numberOfNights);
    context.setVariable(
        "formattedPrice",
        formatPrice(
            reservation.getTotalPrice(),
            reservation.getPaymentCurrency(),
            reservation.getPaymentExchangeRate()));

    String htmlBody = templateEngine.process("owner-reservation-cancelled", context);
    send(ownerEmail, subject, htmlBody, String.valueOf(reservation.getId()));
  }

  public void sendOwnerReservationUpcoming(Reservation reservation) {
    String subject = "Stay Starting Tomorrow Alert";
    String ownerEmail = fetchOwnerEmail(reservation.getUnitId());
    String propertyName = fetchPropertyName(reservation.getUnitId());
    long numberOfNights =
        ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate());

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("reservation", reservation);
    context.setVariable("propertyName", propertyName);
    context.setVariable("numberOfNights", numberOfNights);
    context.setVariable(
        "formattedPrice",
        formatPrice(
            reservation.getTotalPrice(),
            reservation.getPaymentCurrency(),
            reservation.getPaymentExchangeRate()));

    String htmlBody = templateEngine.process("owner-reservation-upcoming", context);
    send(ownerEmail, subject, htmlBody, String.valueOf(reservation.getId()));
  }

  public void sendOwnerReservationStatusChanged(
      Reservation reservation, ReservationStatus oldStatus, ReservationStatus newStatus) {
    String subject = "Reservation Status Changed Alert";
    String ownerEmail = fetchOwnerEmail(reservation.getUnitId());
    String propertyName = fetchPropertyName(reservation.getUnitId());
    long numberOfNights =
        ChronoUnit.DAYS.between(reservation.getStartDate(), reservation.getEndDate());

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("reservation", reservation);
    context.setVariable("propertyName", propertyName);
    context.setVariable("numberOfNights", numberOfNights);
    context.setVariable("oldStatus", oldStatus.name());
    context.setVariable("newStatus", newStatus.name());
    context.setVariable(
        "formattedPrice",
        formatPrice(
            reservation.getTotalPrice(),
            reservation.getPaymentCurrency(),
            reservation.getPaymentExchangeRate()));

    String htmlBody = templateEngine.process("owner-reservation-status-changed", context);
    send(ownerEmail, subject, htmlBody, String.valueOf(reservation.getId()));
  }

  private String formatPrice(
      java.math.BigDecimal totalPrice, String currency, java.math.BigDecimal rate) {
    if (totalPrice == null) return "0.00 PLN";
    String formatted = String.format(java.util.Locale.US, "%.2f PLN", totalPrice);
    if (currency != null
        && !"PLN".equalsIgnoreCase(currency)
        && rate != null
        && rate.compareTo(java.math.BigDecimal.ZERO) > 0) {
      java.math.BigDecimal converted = totalPrice.divide(rate, 2, java.math.RoundingMode.HALF_UP);
      formatted +=
          String.format(
              java.util.Locale.US,
              " (%.2f %s)",
              converted,
              currency.toUpperCase(java.util.Locale.ROOT));
    }
    return formatted;
  }

  private String fetchPropertyName(UUID unitId) {
    if (restTemplate == null) {
      return "Room " + unitId.toString().substring(0, 8);
    }
    try {
      String url = "http://property-service/api/properties/units/" + unitId;
      java.util.Map<?, ?> response = restTemplate.getForObject(url, java.util.Map.class);
      if (response != null && response.get("name") != null) {
        return (String) response.get("name");
      }
    } catch (Exception e) {
      log.warn("Failed to fetch property unit name for unit {}: {}", unitId, e.getMessage());
    }
    return "Room " + unitId.toString().substring(0, 8);
  }

  private String fetchOwnerEmail(UUID unitId) {
    if (restTemplate == null) {
      return "owner1@example.com";
    }
    try {
      String baseUrl = "http://property-service/api/properties";
      String unitUrl = baseUrl + "/units/" + unitId;
      java.util.Map<?, ?> unitResponse = restTemplate.getForObject(unitUrl, java.util.Map.class);
      if (unitResponse != null && unitResponse.get("propertyId") != null) {
        String propertyId = unitResponse.get("propertyId").toString();
        String propertyUrl = baseUrl + "/" + propertyId;
        java.util.Map<?, ?> propertyResponse =
            restTemplate.getForObject(propertyUrl, java.util.Map.class);
        if (propertyResponse != null && propertyResponse.get("ownerId") != null) {
          String ownerIdStr = propertyResponse.get("ownerId").toString();
          if ("22222222-2222-2222-2222-222222222222".equals(ownerIdStr)) {
            return "owner1@example.com";
          } else if ("33333333-3333-3333-3333-333333333333".equals(ownerIdStr)) {
            return "owner2@example.com";
          }
          return "owner_" + ownerIdStr.substring(0, 8) + "@example.com";
        }
      }
    } catch (Exception e) {
      log.warn(
          "Failed to fetch owner email from property-service for unit {}: {}",
          unitId,
          e.getMessage());
    }
    return "owner1@example.com";
  }

  private String getStatusStyle(ReservationStatus status) {
    if (status == null) {
      return "background-color: #F7F7F7; color: #7A7A7A; border: 1px solid #DACDCA;";
    }
    return switch (status) {
      case PENDING -> "background-color: #FEF3C7; color: #92400E;";
      case CONFIRMED -> "background-color: #D1FAE5; color: #065F46;";
      case CANCELLED -> "background-color: #FEE2E2; color: #991B1B;";
      case COMPLETED -> "background-color: #F9F5F5; color: #42211D; border: 1px solid #DACDCA;";
    };
  }

  private void send(String recipientEmail, String subject, String htmlBody, String referenceId) {
    String recipient = resolveRecipient(recipientEmail, subject, referenceId);
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(htmlBody, true);

      mailSender.send(message);
      log.info("Sent email notification '{}' for {}", subject, referenceId);
    } catch (MailException | MessagingException e) {
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
