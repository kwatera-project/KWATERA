package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailNotificationServiceTest {

  private final JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
  private final EmailNotificationService service =
      new EmailNotificationService(mailSender, "no-reply@kwatera.local", "guest@kwatera.local");

  @Test
  void shouldSendSettlementCreatedEmail() {
    Settlement settlement = settlement();

    service.sendSettlementCreated(settlement, "actual.guest@example.com");

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());

    SimpleMailMessage message = captor.getValue();
    assertEquals("no-reply@kwatera.local", message.getFrom());
    assertEquals("actual.guest@example.com", message.getTo()[0]);
    assertEquals("Settlement issued", message.getSubject());
    assertTrue(message.getText().contains(settlement.getReservationId().toString()));
    assertTrue(message.getText().contains("Balance due: 500.00 PLN"));
  }

  @Test
  void shouldSendPaymentStatusChangedEmail() {
    Settlement settlement = settlement();

    service.sendPaymentStatusChanged(
        settlement, SettlementStatus.ISSUED, SettlementStatus.PAID, "actual.guest@example.com");

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());

    SimpleMailMessage message = captor.getValue();
    assertEquals("Payment status changed", message.getSubject());
    assertTrue(message.getText().contains("Previous status: ISSUED"));
    assertTrue(message.getText().contains("New status: PAID"));
  }

  @Test
  void shouldUseFallbackRecipientWhenRecipientEmailIsMissing() {
    service.sendSettlementCreated(settlement(), "");

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());

    assertEquals("guest@kwatera.local", captor.getValue().getTo()[0]);
  }

  @Test
  void shouldNotThrowWhenMailSendingFails() {
    doThrow(new MailSendException("SMTP unavailable"))
        .when(mailSender)
        .send(any(SimpleMailMessage.class));

    assertDoesNotThrow(
        () -> service.sendSettlementCreated(settlement(), "actual.guest@example.com"));
  }

  private Settlement settlement() {
    Settlement settlement = new Settlement();
    settlement.setId(UUID.randomUUID());
    settlement.setReservationId(UUID.randomUUID());
    settlement.setStatus(SettlementStatus.DRAFT);
    settlement.setTotalAmount(new BigDecimal("500.00"));
    settlement.setAmountPaid(BigDecimal.ZERO);
    settlement.setBalanceDue(new BigDecimal("500.00"));
    return settlement;
  }
}
