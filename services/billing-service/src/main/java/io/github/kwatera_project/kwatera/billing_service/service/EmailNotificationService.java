package io.github.kwatera_project.kwatera.billing_service.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
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
  private final String propertyServiceUrl;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public EmailNotificationService(
      JavaMailSender mailSender,
      TemplateEngine templateEngine,
      RestTemplate restTemplate,
      @Value("${kwatera.mail.from}") String fromAddress,
      @Value("${kwatera.mail.test-recipient}") String testRecipient,
      @Value("${services.property.url}") String propertyServiceUrl) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.restTemplate = restTemplate;
    this.fromAddress = fromAddress;
    this.testRecipient = testRecipient;
    this.propertyServiceUrl = propertyServiceUrl;
  }

  public void sendSettlementCreated(Settlement settlement, String recipientEmail) {
    String subject = "Settlement issued";
    ReservationDto reservation = fetchReservation(settlement.getReservationId());
    String email =
        (reservation != null && reservation.getGuestEmail() != null)
            ? reservation.getGuestEmail()
            : recipientEmail;

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("title", "Settlement Invoice Issued");
    context.setVariable(
        "message",
        "A new settlement invoice has been generated for your reservation. Please find the detailed statement below:");
    context.setVariable("settlement", settlement);
    context.setVariable("statusLabel", settlement.getStatus().name());
    context.setVariable("statusStyle", getStatusStyle(settlement.getStatus()));
    context.setVariable("oldStatus", null);
    context.setVariable("paymentUrl", "http://localhost:5173/settlements/" + settlement.getId());

    setupFormattedAmounts(context, settlement, reservation);

    String htmlBody = templateEngine.process("settlement-invoice", context);
    send(email, subject, htmlBody, String.valueOf(settlement.getId()));
  }

  public void sendPaymentStatusChanged(
      Settlement settlement,
      SettlementStatus oldStatus,
      SettlementStatus newStatus,
      String recipientEmail) {
    String subject = "Payment status changed";
    ReservationDto reservation = fetchReservation(settlement.getReservationId());
    String email =
        (reservation != null && reservation.getGuestEmail() != null)
            ? reservation.getGuestEmail()
            : recipientEmail;

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("title", "Payment Status Updated");
    context.setVariable(
        "message",
        "Your reservation settlement payment status was updated. Please review the details of the change below:");
    context.setVariable("settlement", settlement);
    context.setVariable("statusLabel", newStatus.name());
    context.setVariable("statusStyle", getStatusStyle(newStatus));
    context.setVariable("oldStatus", oldStatus.name());
    context.setVariable("newStatus", newStatus.name());
    context.setVariable("paymentUrl", "http://localhost:5173/settlements/" + settlement.getId());

    setupFormattedAmounts(context, settlement, reservation);

    String htmlBody = templateEngine.process("settlement-invoice", context);
    send(email, subject, htmlBody, String.valueOf(settlement.getId()));
  }

  public void sendOwnerPaymentStatusChanged(
      Settlement settlement, SettlementStatus oldStatus, SettlementStatus newStatus, UUID unitId) {
    String subject = "Guest Payment Alert";
    String ownerEmail = fetchOwnerEmail(unitId);
    ReservationDto reservation = fetchReservation(settlement.getReservationId());

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("title", "Settlement Payment Status Updated");
    context.setVariable(
        "message",
        "A payment status update occurred for a stay at your property. Below are the settlement details:");
    context.setVariable("settlement", settlement);
    context.setVariable("statusLabel", newStatus.name());
    context.setVariable("statusStyle", getStatusStyle(newStatus));
    context.setVariable("oldStatus", oldStatus.name());
    context.setVariable("newStatus", newStatus.name());

    setupFormattedAmounts(context, settlement, reservation);

    String htmlBody = templateEngine.process("owner-payment-status", context);
    send(ownerEmail, subject, htmlBody, String.valueOf(settlement.getId()));
  }

  public void sendUtilityChargesAdded(Settlement settlement, SettlementItem item) {
    String subject = "New utility charges added";
    ReservationDto reservation = fetchReservation(settlement.getReservationId());
    String email =
        (reservation != null && reservation.getGuestEmail() != null)
            ? reservation.getGuestEmail()
            : null;

    Context context = new Context();
    context.setVariable("subject", subject);
    context.setVariable("title", "Utility Charges Calculated");
    context.setVariable(
        "message",
        "Your final meter readings have been processed, and the utility charges have been added to your reservation settlement. Please find the details below:");
    context.setVariable("settlement", settlement);
    context.setVariable("item", item);
    context.setVariable("statusLabel", settlement.getStatus().name());
    context.setVariable("statusStyle", getStatusStyle(settlement.getStatus()));
    context.setVariable("paymentUrl", "http://localhost:5173/settlements/" + settlement.getId());

    setupFormattedAmounts(context, settlement, reservation);

    String currency = "PLN";
    BigDecimal rate = BigDecimal.ONE;
    if (reservation != null && reservation.getCurrencyInfo() != null) {
      if (reservation.getCurrencyInfo().displayCurrency() != null) {
        currency = reservation.getCurrencyInfo().displayCurrency();
      }
      if (reservation.getCurrencyInfo().exchangeRate() != null) {
        rate = reservation.getCurrencyInfo().exchangeRate();
      }
    }
    context.setVariable("formattedItemAmount", formatPrice(item.getAmount(), currency, rate));

    String htmlBody = templateEngine.process("utility-charges-added", context);
    send(email, subject, htmlBody, String.valueOf(settlement.getId()));
  }

  private ReservationDto fetchReservation(UUID reservationId) {
    if (restTemplate == null) {
      return null;
    }
    try {
      String url = "http://reservation-service/api/v1/reservations/internal/" + reservationId;
      return restTemplate.getForObject(url, ReservationDto.class);
    } catch (Exception e) {
      log.warn(
          "Failed to fetch reservation details internally for {}: {}",
          reservationId,
          e.getMessage());
      return null;
    }
  }

  private void setupFormattedAmounts(
      Context context, Settlement settlement, ReservationDto reservation) {
    String currency = "PLN";
    BigDecimal rate = BigDecimal.ONE;
    if (reservation != null && reservation.getCurrencyInfo() != null) {
      if (reservation.getCurrencyInfo().displayCurrency() != null) {
        currency = reservation.getCurrencyInfo().displayCurrency();
      }
      if (reservation.getCurrencyInfo().exchangeRate() != null) {
        rate = reservation.getCurrencyInfo().exchangeRate();
      }
    }

    context.setVariable(
        "formattedAccommodation", formatPrice(settlement.getAccommodationAmount(), currency, rate));
    context.setVariable(
        "formattedUtilities", formatPrice(settlement.getUtilitiesAmount(), currency, rate));
    context.setVariable(
        "formattedDeposit", formatPrice(settlement.getDepositAmount(), currency, rate));
    context.setVariable(
        "formattedDiscount", formatPrice(settlement.getDiscountAmount(), currency, rate));
    context.setVariable("formattedTotal", formatPrice(settlement.getTotalAmount(), currency, rate));
    context.setVariable("formattedPaid", formatPrice(settlement.getAmountPaid(), currency, rate));
    context.setVariable(
        "formattedBalanceDue", formatPrice(settlement.getBalanceDue(), currency, rate));
  }

  private String formatPrice(BigDecimal amount, String currency, BigDecimal rate) {
    if (amount == null) return "0.00 PLN";
    String formatted = String.format(java.util.Locale.US, "%.2f PLN", amount);
    if (currency != null
        && !"PLN".equalsIgnoreCase(currency)
        && rate != null
        && rate.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal converted = amount.divide(rate, 2, java.math.RoundingMode.HALF_UP);
      formatted +=
          String.format(
              java.util.Locale.US,
              " (%.2f %s)",
              converted,
              currency.toUpperCase(java.util.Locale.ROOT));
    }
    return formatted;
  }

  private String fetchOwnerEmail(UUID unitId) {
    if (restTemplate == null) {
      return "owner1@example.com";
    }
    try {
      String baseUrl = propertyServiceUrl;
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

  private String getStatusStyle(SettlementStatus status) {
    if (status == null) {
      return "background-color: #F7F7F7; color: #7A7A7A; border: 1px solid #DACDCA;";
    }
    return switch (status) {
      case DRAFT -> "background-color: #F3F4F6; color: #4B5563;";
      case ISSUED -> "background-color: #F9F5F5; color: #42211D; border: 1px solid #DACDCA;";
      case PARTIALLY_PAID -> "background-color: #FEF3C7; color: #92400E;";
      case PAID -> "background-color: #D1FAE5; color: #065F46;";
      case CANCELLED -> "background-color: #FEE2E2; color: #991B1B;";
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
      log.warn("Failed to send email notification '{}' for {}", subject, referenceId, e);
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
