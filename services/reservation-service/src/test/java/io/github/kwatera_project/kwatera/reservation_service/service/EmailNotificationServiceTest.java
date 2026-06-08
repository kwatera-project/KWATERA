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
          "guest@kwatera.local");

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
  void shouldSendReservationCreatedEmailWithForeignCurrency() throws Exception {
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
  void shouldNotThrowWhenMailSendingFails() {
    doThrow(new MailSendException("SMTP unavailable"))
        .when(mailSender)
        .send(any(MimeMessage.class));

    assertDoesNotThrow(
        () -> service.sendReservationCreated(reservation(), "actual.guest@example.com"));
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
