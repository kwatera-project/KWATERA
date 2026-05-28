package io.github.kwatera_project.kwatera.reservation_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.github.kwatera_project.kwatera.reservation_service.model.Reservation;
import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
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
  void shouldSendReservationCreatedEmail() {
    Reservation reservation = reservation();

    service.sendReservationCreated(reservation, "actual.guest@example.com");

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());

    SimpleMailMessage message = captor.getValue();
    assertEquals("no-reply@kwatera.local", message.getFrom());
    assertEquals("actual.guest@example.com", message.getTo()[0]);
    assertEquals("Reservation created", message.getSubject());
    assertTrue(message.getText().contains(reservation.getId().toString()));
    assertTrue(message.getText().contains("Total price: 400.00 PLN"));
  }

  @Test
  void shouldSendReservationStatusChangedEmail() {
    Reservation reservation = reservation();

    service.sendReservationStatusChanged(
        reservation,
        ReservationStatus.PENDING,
        ReservationStatus.CONFIRMED,
        "actual.guest@example.com");

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());

    SimpleMailMessage message = captor.getValue();
    assertEquals("Reservation status changed", message.getSubject());
    assertTrue(message.getText().contains("Previous status: PENDING"));
    assertTrue(message.getText().contains("New status: CONFIRMED"));
  }

  @Test
  void shouldUseFallbackRecipientWhenRecipientEmailIsMissing() {
    service.sendReservationCreated(reservation(), " ");

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
