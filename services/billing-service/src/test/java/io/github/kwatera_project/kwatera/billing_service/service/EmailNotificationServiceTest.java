package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementStatus;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

class EmailNotificationServiceTest {

  private final JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
  private final TemplateEngine templateEngine = org.mockito.Mockito.mock(TemplateEngine.class);
  private final RestTemplate restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
  private final EmailNotificationService service =
      new EmailNotificationService(
          mailSender,
          templateEngine,
          restTemplate,
          "no-reply@kwatera.local",
          "guest@kwatera.local",
          "http://property-service/api/properties");

  private MimeMessage mimeMessage;

  @BeforeEach
  void setUp() {
    mimeMessage = new MimeMessage((jakarta.mail.Session) null);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(any(String.class), any(Context.class)))
        .thenReturn("<html>mocked body</html>");
  }

  @Test
  void shouldSendSettlementCreatedEmail() throws Exception {
    Settlement settlement = settlement();

    service.sendSettlementCreated(settlement, "actual.guest@example.com");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals("no-reply@kwatera.local", message.getFrom()[0].toString());
    assertEquals(
        "actual.guest@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("Settlement issued", message.getSubject());
    assertEquals("<html>mocked body</html>", message.getContent());
  }

  @Test
  void shouldSendPaymentStatusChangedEmail() throws Exception {
    Settlement settlement = settlement();

    service.sendPaymentStatusChanged(
        settlement, SettlementStatus.ISSUED, SettlementStatus.PAID, "actual.guest@example.com");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals("Payment status changed", message.getSubject());
    assertEquals("<html>mocked body</html>", message.getContent());
  }

  @Test
  void shouldSendOwnerPaymentStatusChangedEmail() throws Exception {
    Settlement settlement = settlement();

    service.sendOwnerPaymentStatusChanged(
        settlement, SettlementStatus.ISSUED, SettlementStatus.PAID, UUID.randomUUID());

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals("no-reply@kwatera.local", message.getFrom()[0].toString());
    assertEquals(
        "owner1@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("Guest Payment Alert", message.getSubject());
  }

  @Test
  void shouldSendUtilityChargesAddedEmail() throws Exception {
    Settlement settlement = settlement();
    SettlementItem item = new SettlementItem();
    item.setId(UUID.randomUUID());
    item.setSettlementId(settlement.getId());
    item.setType(io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType.WATER);
    item.setDescription("Water usage");
    item.setQuantity(new BigDecimal("10.00"));
    item.setUnitPrice(new BigDecimal("5.00"));
    item.setAmount(new BigDecimal("50.00"));

    service.sendUtilityChargesAdded(settlement, item);

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals("New utility charges added", message.getSubject());
  }

  @Test
  void shouldUseFallbackRecipientWhenRecipientEmailIsMissing() throws Exception {
    service.sendSettlementCreated(settlement(), "");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    assertEquals(
        "guest@kwatera.local",
        captor.getValue().getRecipients(Message.RecipientType.TO)[0].toString());
  }

  @Test
  void shouldNotThrowWhenMailSendingFails() {
    doThrow(new MailSendException("SMTP unavailable"))
        .when(mailSender)
        .send(any(MimeMessage.class));

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
    settlement.setAccommodationAmount(new BigDecimal("500.00"));
    settlement.setUtilitiesAmount(BigDecimal.ZERO);
    settlement.setDepositAmount(BigDecimal.ZERO);
    settlement.setDiscountAmount(BigDecimal.ZERO);
    return settlement;
  }
}
