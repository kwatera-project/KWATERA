package io.github.kwatera_project.kwatera.billing_service.service;

import static org.mockito.Mockito.*;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import io.github.kwatera_project.kwatera.billing_service.client.StripeClient;
import io.github.kwatera_project.kwatera.billing_service.client.SystemEventClient;
import com.stripe.exception.ApiException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

  @Mock private SettlementService settlementService;

  @Mock private StripeClient stripeClient;

  @InjectMocks private PaymentWebhookService paymentWebhookService;

  @Mock private Event event;

  @Mock private EventDataObjectDeserializer deserializer;

  @Mock private Session session;

  @Mock private PaymentTransactionService paymentTransactionService;

  @Mock private SystemEventClient systemEventClient;

  @Mock private PaymentIntent paymentIntent;

  @BeforeEach
  void setup() {
    paymentWebhookService =
        new PaymentWebhookService(
            settlementService, stripeClient, paymentTransactionService, systemEventClient);
    ReflectionTestUtils.setField(paymentWebhookService, "stripeWebhookSecret", "test_secret");
  }

  private Map<String, String> validMetadata() {
    return Map.of(
        "settlementId", UUID.randomUUID().toString(),
        "unitId", UUID.randomUUID().toString(),
        "type", "DEPOSIT",
        "description", "desc",
        "quantity", "1",
        "unitPrice", "40",
        "recipientEmail", "guest@example.com");
  }

  private Event mockEvent(String id, String type) {
    Event event = mock(Event.class);
    when(event.getId()).thenReturn(id);
    when(event.getType()).thenReturn(type);
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);
    return event;
  }

  @Test
  void shouldBeIdempotentAndNotProcessTwice() throws Exception {

    Event event = mockEvent("evt_1", "checkout.session.completed");

    Session session = mock(Session.class);
    when(session.getId()).thenReturn("sess_1");
    when(session.getMetadata()).thenReturn(validMetadata());

    when(deserializer.getObject()).thenReturn(java.util.Optional.of(session));
    when(stripeClient.constructEvent(anyString(), anyString(), anyString())).thenReturn(event);
    when(stripeClient.retrieveSession("sess_1")).thenReturn(session);

    when(paymentTransactionService.createProcessingIfNotExists(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(false);

    paymentWebhookService.processWebhook("payload", "sig");
    paymentWebhookService.processWebhook("payload", "sig");

    verify(settlementService, never())
        .registerPayment(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldProcessCheckoutCompleted() throws Exception {

    Event event = mockEvent("evt_1", "checkout.session.completed");

    Session session = mock(Session.class);
    when(session.getId()).thenReturn("sess_1");
    when(session.getMetadata()).thenReturn(validMetadata());

    when(deserializer.getObject()).thenReturn(Optional.of(session));
    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);
    when(stripeClient.retrieveSession("sess_1")).thenReturn(session);

    when(paymentTransactionService.createProcessingIfNotExists(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);

    paymentWebhookService.processWebhook("payload", "sig");

    verify(settlementService)
        .registerPayment(any(), any(), any(), any(), any(), any(), eq("guest@example.com"));
    verify(settlementService).generateInvoicePdfIfNeeded(any());
    verify(paymentTransactionService).markSuccessIfAllowed("evt_1");
  }

  @Test
  void shouldIgnoreDuplicateCheckoutCompleted() throws Exception {

    Event event = mockEvent("evt_1", "checkout.session.completed");

    Session session = mock(Session.class);
    when(session.getId()).thenReturn("sess_1");
    when(session.getMetadata()).thenReturn(validMetadata());

    when(deserializer.getObject()).thenReturn(Optional.of(session));
    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);
    when(stripeClient.retrieveSession("sess_1")).thenReturn(session);

    when(paymentTransactionService.createProcessingIfNotExists(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(false);

    paymentWebhookService.processWebhook("payload", "sig");

    verifyNoInteractions(settlementService);
  }

  @Test
  void shouldMarkTransactionFailedWhenRegisterPaymentFails() throws Exception {

    Event event = mockEvent("evt_4", "checkout.session.completed");

    Session session = mock(Session.class);
    when(session.getId()).thenReturn("sess_4");
    when(session.getMetadata()).thenReturn(validMetadata());

    when(deserializer.getObject()).thenReturn(Optional.of(session));
    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);
    when(stripeClient.retrieveSession("sess_4")).thenReturn(session);

    when(paymentTransactionService.createProcessingIfNotExists(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);

    doThrow(new RuntimeException("Settlement update failed"))
        .when(settlementService)
        .registerPayment(any(), any(), any(), any(), any(), any(), any());

    org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class, () -> paymentWebhookService.processWebhook("payload", "sig"));

    verify(paymentTransactionService).markFailed(eq("evt_4"), any());
    verify(systemEventClient)
        .logSafely(
            eq("PAYMENT_FAILED"),
            isNull(),
            eq("SETTLEMENT"),
            any(),
            contains("stripeEventId=evt_4"));
    verify(paymentTransactionService, never()).markSuccessIfAllowed("evt_4");
  }

  @Test
  void shouldHandleCheckoutExpired() throws Exception {

    Event event = mockEvent("evt_2", "checkout.session.expired");

    Session session = mock(Session.class);
    when(session.getId()).thenReturn("sess_2");
    when(session.getMetadata()).thenReturn(validMetadata());

    when(deserializer.getObject()).thenReturn(Optional.of(session));
    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);
    when(stripeClient.retrieveSession("sess_2")).thenReturn(session);

    paymentWebhookService.processWebhook("payload", "sig");

    verify(paymentTransactionService).markFailed(eq("evt_2"), any());
    verify(systemEventClient)
        .logSafely(
            eq("PAYMENT_CANCELLED"),
            isNull(),
            eq("SETTLEMENT"),
            any(),
            contains("Checkout session expired"));
  }

  @Test
  void shouldHandleCheckoutAsyncPaymentFailed() throws Exception {
    Event event = mockEvent("evt_5", "checkout.session.async_payment_failed");

    Session session = mock(Session.class);
    when(session.getId()).thenReturn("sess_5");
    when(session.getMetadata()).thenReturn(validMetadata());

    when(deserializer.getObject()).thenReturn(Optional.of(session));
    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);

    paymentWebhookService.processWebhook("payload", "sig");

    verify(paymentTransactionService).markFailed(eq("evt_5"), any());
    verify(systemEventClient)
        .logSafely(
            eq("PAYMENT_FAILED"),
            isNull(),
            eq("SETTLEMENT"),
            any(),
            contains("Checkout payment failed"));
  }

  @Test
  void shouldHandlePaymentFailed() throws Exception {

    Event event = mockEvent("evt_3", "payment_intent.payment_failed");

    PaymentIntent intent = mock(PaymentIntent.class);
    when(intent.getId()).thenReturn("pi_1");
    when(intent.getMetadata()).thenReturn(validMetadata());
    when(intent.getLastPaymentError()).thenReturn(null);

    EventDataObjectDeserializer deser = mock(EventDataObjectDeserializer.class);
    when(event.getDataObjectDeserializer()).thenReturn(deser);
    when(deser.getObject()).thenReturn(Optional.of(intent));

    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);

    paymentWebhookService.processWebhook("payload", "sig");

    verify(paymentTransactionService).markFailed(eq("evt_3"), any());
    verify(systemEventClient)
        .logSafely(
            eq("PAYMENT_FAILED"),
            isNull(),
            eq("SETTLEMENT"),
            any(),
            contains("stripeEventId=evt_3"));
  }

  @Test
  void shouldIgnoreUnknownEventType() throws Exception {

    Event event = mock(Event.class);
    when(event.getId()).thenReturn("evt_99");
    when(event.getType()).thenReturn("unknown.event");

    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);

    paymentWebhookService.processWebhook("payload", "sig");

    verifyNoInteractions(settlementService);
    verifyNoInteractions(paymentTransactionService);
  }

  @Test
  void shouldFallbackToEventSessionWhenRetrieveSessionThrows() throws Exception {
    Event event = mockEvent("evt_fallback_1", "checkout.session.completed");
    Session eventSession = mock(Session.class);
    when(eventSession.getId()).thenReturn("sess_fallback_1");
    when(eventSession.getMetadata()).thenReturn(validMetadata());

    when(deserializer.getObject()).thenReturn(Optional.of(eventSession));
    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);
    when(stripeClient.retrieveSession("sess_fallback_1"))
        .thenThrow(new ApiException("Stripe error", null, null, 500, null));

    when(paymentTransactionService.createProcessingIfNotExists(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);

    paymentWebhookService.processWebhook("payload", "sig");

    verify(settlementService).registerPayment(any(), any(), any(), any(), any(), any(), eq("guest@example.com"));
    verify(settlementService).generateInvoicePdfIfNeeded(any());
    verify(paymentTransactionService).markSuccessIfAllowed("evt_fallback_1");
  }

  @Test
  void shouldFallbackToEventSessionWhenRetrievedSessionHasEmptyMetadata() throws Exception {
    Event event = mockEvent("evt_fallback_2", "checkout.session.completed");
    Session eventSession = mock(Session.class);
    when(eventSession.getId()).thenReturn("sess_fallback_2");
    when(eventSession.getMetadata()).thenReturn(validMetadata());

    Session retrievedSession = mock(Session.class);
    when(retrievedSession.getMetadata()).thenReturn(null);

    when(deserializer.getObject()).thenReturn(Optional.of(eventSession));
    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);
    when(stripeClient.retrieveSession("sess_fallback_2")).thenReturn(retrievedSession);

    when(paymentTransactionService.createProcessingIfNotExists(
            any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);

    paymentWebhookService.processWebhook("payload", "sig");

    verify(settlementService).registerPayment(any(), any(), any(), any(), any(), any(), eq("guest@example.com"));
    verify(settlementService).generateInvoicePdfIfNeeded(any());
    verify(paymentTransactionService).markSuccessIfAllowed("evt_fallback_2");
  }
}
