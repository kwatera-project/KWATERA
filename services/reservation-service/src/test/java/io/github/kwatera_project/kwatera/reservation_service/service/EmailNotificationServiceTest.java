package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
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
  void shouldSendReservationCreatedEmail() throws Exception {
    Reservation reservation = reservation();

    service.sendReservationCreated(reservation, "actual.guest@example.com");

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine)
        .process(
            org.mockito.ArgumentMatchers.eq("reservation-confirmation"), contextCaptor.capture());
    Context context = contextCaptor.getValue();
    assertEquals("400.00 PLN", context.getVariable("formattedPrice"));

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals("no-reply@kwatera.local", message.getFrom()[0].toString());
    assertEquals(
        "actual.guest@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("Reservation created", message.getSubject());
    assertEquals("<html>mocked body</html>", message.getContent());
  }

  @Test
  void shouldSendReservationCreatedEmailWithForeignCurrency() {
    Reservation reservation = reservation();
    reservation.setPaymentCurrency("USD");
    reservation.setPaymentExchangeRate(new BigDecimal("4.00"));

    service.sendReservationCreated(reservation, "actual.guest@example.com");

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine)
        .process(
            org.mockito.ArgumentMatchers.eq("reservation-confirmation"), contextCaptor.capture());
    Context context = contextCaptor.getValue();
    assertEquals("400.00 PLN (100.00 USD)", context.getVariable("formattedPrice"));
  }

  @Test
  void shouldUseBlockedStatusStyleInGuestNotification() {
    Reservation reservation = reservation();
    reservation.setStatus(ReservationStatus.BLOCKED);

    service.sendReservationCreated(reservation, "actual.guest@example.com");

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine)
        .process(
            org.mockito.ArgumentMatchers.eq("reservation-confirmation"), contextCaptor.capture());

    Context context = contextCaptor.getValue();

    assertEquals("BLOCKED", context.getVariable("statusLabel"));
    assertEquals("background-color: #F3F4F6; color: #374151;", context.getVariable("statusStyle"));
  }

  @Test
  void shouldSendReservationStatusChangedEmail() throws Exception {
    Reservation reservation = reservation();

    service.sendReservationStatusChanged(
        reservation,
        ReservationStatus.PENDING,
        ReservationStatus.CONFIRMED,
        "actual.guest@example.com");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals("Reservation status changed", message.getSubject());
    assertEquals("<html>mocked body</html>", message.getContent());
  }

  @Test
  void shouldSendOwnerReservationCreatedEmail() throws Exception {
    Reservation reservation = reservation();

    service.sendOwnerReservationCreated(reservation);

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals("no-reply@kwatera.local", message.getFrom()[0].toString());
    assertEquals(
        "owner1@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("New booking Alert", message.getSubject());
  }

  @Test
  void shouldSendOwnerReservationCancelledEmail() throws Exception {
    Reservation reservation = reservation();

    service.sendOwnerReservationCancelled(reservation);

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals(
        "owner1@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("Reservation Cancelled Alert", message.getSubject());
  }

  @Test
  void shouldSendOwnerReservationUpcomingEmail() throws Exception {
    Reservation reservation = reservation();

    service.sendOwnerReservationUpcoming(reservation);

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals(
        "owner1@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("Stay Starting Tomorrow Alert", message.getSubject());
  }

  @Test
  void shouldSendOwnerReservationStatusChangedEmail() throws Exception {
    Reservation reservation = reservation();

    service.sendOwnerReservationStatusChanged(
        reservation, ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    MimeMessage message = captor.getValue();
    assertEquals(
        "owner1@example.com", message.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("Reservation Status Changed Alert", message.getSubject());
  }

  @Test
  void shouldUseFallbackRecipientWhenRecipientEmailIsMissing() throws Exception {
    service.sendReservationCreated(reservation(), " ");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());

    assertEquals(
        "guest@kwatera.local",
        captor.getValue().getRecipients(Message.RecipientType.TO)[0].toString());
  }

  @Test
  void shouldUseFallbackRecipientWhenRecipientEmailIsNull() throws Exception {
    service.sendReservationCreated(reservation(), null);

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
        () -> service.sendReservationCreated(reservation(), "actual.guest@example.com"));
  }

  @Test
  void shouldNotThrowWhenMailSendingFailsWithNullMessage() {
    doThrow(new MailSendException((String) null)).when(mailSender).send(any(MimeMessage.class));

    assertDoesNotThrow(
        () -> service.sendReservationCreated(reservation(), "actual.guest@example.com"));
  }

  @Test
  void shouldFetchPropertyNameSuccessfully() throws Exception {
    Reservation reservation = reservation();
    java.util.Map<String, Object> response = new java.util.HashMap<>();
    response.put("name", "Mock Property Name");

    when(restTemplate.getForObject(
            "http://property-service/api/properties/units/" + reservation.getUnitId(),
            java.util.Map.class))
        .thenReturn(response);

    service.sendReservationCreated(reservation, "actual.guest@example.com");

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine, org.mockito.Mockito.atLeastOnce())
        .process(
            org.mockito.ArgumentMatchers.eq("reservation-confirmation"), contextCaptor.capture());
    Context context = contextCaptor.getValue();
    assertEquals("Mock Property Name", context.getVariable("propertyName"));
  }

  @Test
  void shouldHandleExceptionWhenFetchingPropertyName() {
    Reservation reservation = reservation();
    when(restTemplate.getForObject(
            "http://property-service/api/properties/units/" + reservation.getUnitId(),
            java.util.Map.class))
        .thenThrow(new RuntimeException("Property Service down"));

    assertDoesNotThrow(
        () -> service.sendReservationCreated(reservation, "actual.guest@example.com"));
  }

  @Test
  void shouldFetchOwnerEmailSuccessfullyFromPropertyService() throws Exception {
    Reservation reservation = reservation();

    java.util.Map<String, Object> unitResponse = new java.util.HashMap<>();
    unitResponse.put("propertyId", UUID.randomUUID());

    java.util.Map<String, Object> propertyResponse = new java.util.HashMap<>();
    propertyResponse.put("ownerId", "22222222-2222-2222-2222-222222222222");

    java.util.Map<String, Object> owner1Response = new java.util.HashMap<>();
    owner1Response.put("email", "owner1@example.com");

    when(restTemplate.getForObject(
            "http://property-service/api/properties/units/" + reservation.getUnitId(),
            java.util.Map.class))
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

    service.sendOwnerReservationCreated(reservation);

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

    service.sendOwnerReservationCreated(reservation);

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

    service.sendOwnerReservationCreated(reservation);

    verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
    assertEquals(
        "owner_44444444@example.com",
        captor.getValue().getRecipients(Message.RecipientType.TO)[0].toString());
  }

  @Test
  void shouldHandleExceptionWhenFetchingOwnerEmail() {
    Reservation reservation = reservation();

    when(restTemplate.getForObject(
            "http://property-service/api/properties/units/" + reservation.getUnitId(),
            java.util.Map.class))
        .thenThrow(new RuntimeException("Property Service down"));

    assertDoesNotThrow(() -> service.sendOwnerReservationCreated(reservation));
  }

  @Test
  void shouldHandlePriceFormattingEdgeCases() throws Exception {
    Reservation reservation = reservation();
    reservation.setTotalPrice(null);

    service.sendReservationCreated(reservation, "original.guest@example.com");

    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine, org.mockito.Mockito.atLeastOnce())
        .process(
            org.mockito.ArgumentMatchers.eq("reservation-confirmation"), contextCaptor.capture());
    Context context = contextCaptor.getValue();
    assertEquals("0.00 PLN", context.getVariable("formattedPrice"));
  }

  private Reservation reservation() {
    Reservation reservation = new Reservation();
    reservation.setId(UUID.randomUUID());
    reservation.setUserId(UUID.randomUUID());
    reservation.setUnitId(UUID.randomUUID());
    reservation.setStartDate(LocalDate.now().plusDays(1));
    reservation.setEndDate(LocalDate.now().plusDays(3));
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setTotalPrice(new BigDecimal("400.00"));
    return reservation;
  }
}
