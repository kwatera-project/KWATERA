package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.dto.CurrencyMetadataDto;
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
          "http://property-service/api/properties",
          "http://auth-service/api/auth/users",
          "test-internal-token",
          "http://localhost:5173",
          "owner1@example.com");

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
  void shouldUseFallbackRecipientWhenRecipientEmailIsNull() throws Exception {
    service.sendSettlementCreated(settlement(), null);

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

  @Test
  void shouldNotThrowWhenMailSendingFailsWithNullMessage() {
    doThrow(new MailSendException((String) null)).when(mailSender).send(any(MimeMessage.class));

    assertDoesNotThrow(
        () -> service.sendSettlementCreated(settlement(), "actual.guest@example.com"));
  }

  @Test
  void shouldFetchReservationSuccessfullyWithPlnAndEur() throws Exception {
    UUID reservationId = UUID.randomUUID();
    Settlement settlement = settlement();
    settlement.setReservationId(reservationId);

    io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto reservationDtoPln =
        new io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto();
    reservationDtoPln.setId(reservationId);
    reservationDtoPln.setGuestEmail("guest.pln@example.com");
    reservationDtoPln.setCurrencyInfo(new CurrencyMetadataDto("PLN", "PLN", BigDecimal.ONE, null));

    when(restTemplate.exchange(
            org.mockito.Mockito.eq(
                "http://reservation-service/api/v1/reservations/internal/" + reservationId),
            org.mockito.Mockito.eq(org.springframework.http.HttpMethod.GET),
            any(org.springframework.http.HttpEntity.class),
            org.mockito.Mockito.eq(
                io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto.class)))
        .thenReturn(
            new org.springframework.http.ResponseEntity<>(
                reservationDtoPln, org.springframework.http.HttpStatus.OK));

    service.sendSettlementCreated(settlement, "original.guest@example.com");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
    assertEquals(
        "guest.pln@example.com",
        captor.getValue().getRecipients(Message.RecipientType.TO)[0].toString());

    io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto reservationDtoEur =
        new io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto();
    reservationDtoEur.setId(reservationId);
    reservationDtoEur.setGuestEmail("guest.eur@example.com");
    reservationDtoEur.setCurrencyInfo(
        new CurrencyMetadataDto("PLN", "EUR", new BigDecimal("4.50"), null));

    when(restTemplate.exchange(
            org.mockito.Mockito.eq(
                "http://reservation-service/api/v1/reservations/internal/" + reservationId),
            org.mockito.Mockito.eq(org.springframework.http.HttpMethod.GET),
            any(org.springframework.http.HttpEntity.class),
            org.mockito.Mockito.eq(
                io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto.class)))
        .thenReturn(
            new org.springframework.http.ResponseEntity<>(
                reservationDtoEur, org.springframework.http.HttpStatus.OK));

    service.sendSettlementCreated(settlement, "original.guest@example.com");
  }

  @Test
  void shouldHandleExceptionWhenFetchingReservation() {
    UUID reservationId = UUID.randomUUID();
    Settlement settlement = settlement();
    settlement.setReservationId(reservationId);

    when(restTemplate.exchange(
            org.mockito.Mockito.eq(
                "http://reservation-service/api/v1/reservations/internal/" + reservationId),
            org.mockito.Mockito.eq(org.springframework.http.HttpMethod.GET),
            any(org.springframework.http.HttpEntity.class),
            org.mockito.Mockito.eq(
                io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto.class)))
        .thenThrow(new RuntimeException("Service down"));

    assertDoesNotThrow(() -> service.sendSettlementCreated(settlement, "actual.guest@example.com"));
  }

  @Test
  void shouldFetchOwnerEmailSuccessfullyFromPropertyService() throws Exception {
    UUID unitId = UUID.randomUUID();
    Settlement settlement = settlement();

    java.util.Map<String, Object> unitResponse = new java.util.HashMap<>();
    unitResponse.put("propertyId", UUID.randomUUID());

    java.util.Map<String, Object> propertyResponse = new java.util.HashMap<>();
    propertyResponse.put("ownerId", "22222222-2222-2222-2222-222222222222");

    java.util.Map<String, Object> owner1Response = new java.util.HashMap<>();
    owner1Response.put("email", "owner1@example.com");

    when(restTemplate.getForObject(
            "http://property-service/api/properties/units/" + unitId, java.util.Map.class))
        .thenReturn(unitResponse);
    when(restTemplate.getForObject(
            "http://property-service/api/properties/" + unitResponse.get("propertyId"),
            java.util.Map.class))
        .thenReturn(propertyResponse);
    when(restTemplate.exchange(
            org.mockito.Mockito.eq(
                "http://auth-service/api/auth/users/internal/22222222-2222-2222-2222-222222222222"),
            org.mockito.Mockito.eq(org.springframework.http.HttpMethod.GET),
            any(org.springframework.http.HttpEntity.class),
            org.mockito.Mockito.eq(java.util.Map.class)))
        .thenReturn(
            new org.springframework.http.ResponseEntity<>(
                owner1Response, org.springframework.http.HttpStatus.OK));

    service.sendOwnerPaymentStatusChanged(
        settlement, SettlementStatus.ISSUED, SettlementStatus.PAID, unitId);

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
    assertEquals(
        "owner1@example.com",
        captor.getValue().getRecipients(Message.RecipientType.TO)[0].toString());

    propertyResponse.put("ownerId", "33333333-3333-3333-3333-333333333333");
    java.util.Map<String, Object> owner2Response = new java.util.HashMap<>();
    owner2Response.put("email", "owner2@example.com");
    when(restTemplate.exchange(
            org.mockito.Mockito.eq(
                "http://auth-service/api/auth/users/internal/33333333-3333-3333-3333-333333333333"),
            org.mockito.Mockito.eq(org.springframework.http.HttpMethod.GET),
            any(org.springframework.http.HttpEntity.class),
            org.mockito.Mockito.eq(java.util.Map.class)))
        .thenReturn(
            new org.springframework.http.ResponseEntity<>(
                owner2Response, org.springframework.http.HttpStatus.OK));

    service.sendOwnerPaymentStatusChanged(
        settlement, SettlementStatus.ISSUED, SettlementStatus.PAID, unitId);

    verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
    assertEquals(
        "owner2@example.com",
        captor.getValue().getRecipients(Message.RecipientType.TO)[0].toString());

    propertyResponse.put("ownerId", "44444444-4444-4444-4444-444444444444");
    java.util.Map<String, Object> owner4Response = new java.util.HashMap<>();
    owner4Response.put("email", "owner_44444444@example.com");
    when(restTemplate.exchange(
            org.mockito.Mockito.eq(
                "http://auth-service/api/auth/users/internal/44444444-4444-4444-4444-444444444444"),
            org.mockito.Mockito.eq(org.springframework.http.HttpMethod.GET),
            any(org.springframework.http.HttpEntity.class),
            org.mockito.Mockito.eq(java.util.Map.class)))
        .thenReturn(
            new org.springframework.http.ResponseEntity<>(
                owner4Response, org.springframework.http.HttpStatus.OK));

    service.sendOwnerPaymentStatusChanged(
        settlement, SettlementStatus.ISSUED, SettlementStatus.PAID, unitId);

    verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
    assertEquals(
        "owner_44444444@example.com",
        captor.getValue().getRecipients(Message.RecipientType.TO)[0].toString());
  }

  @Test
  void shouldHandleExceptionWhenFetchingOwnerEmail() {
    UUID unitId = UUID.randomUUID();
    Settlement settlement = settlement();

    when(restTemplate.getForObject(
            "http://property-service/api/properties/units/" + unitId, java.util.Map.class))
        .thenThrow(new RuntimeException("Property Service down"));

    assertDoesNotThrow(
        () ->
            service.sendOwnerPaymentStatusChanged(
                settlement, SettlementStatus.ISSUED, SettlementStatus.PAID, unitId));
  }

  @Test
  void shouldHandlePriceFormattingEdgeCases() throws Exception {
    UUID reservationId = UUID.randomUUID();
    Settlement settlement = settlement();
    settlement.setReservationId(reservationId);
    settlement.setAccommodationAmount(null);
    settlement.setUtilitiesAmount(null);
    settlement.setDepositAmount(null);
    settlement.setDiscountAmount(null);
    settlement.setTotalAmount(null);
    settlement.setAmountPaid(null);
    settlement.setBalanceDue(null);

    io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto reservationDtoZeroRate =
        new io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto();
    reservationDtoZeroRate.setId(reservationId);
    reservationDtoZeroRate.setGuestEmail("guest.zero@example.com");
    reservationDtoZeroRate.setCurrencyInfo(
        new CurrencyMetadataDto("PLN", "EUR", BigDecimal.ZERO, null));

    when(restTemplate.exchange(
            org.mockito.Mockito.eq(
                "http://reservation-service/api/v1/reservations/internal/" + reservationId),
            org.mockito.Mockito.eq(org.springframework.http.HttpMethod.GET),
            any(org.springframework.http.HttpEntity.class),
            org.mockito.Mockito.eq(
                io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto.class)))
        .thenReturn(
            new org.springframework.http.ResponseEntity<>(
                reservationDtoZeroRate, org.springframework.http.HttpStatus.OK));

    service.sendSettlementCreated(settlement, "original.guest@example.com");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
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
