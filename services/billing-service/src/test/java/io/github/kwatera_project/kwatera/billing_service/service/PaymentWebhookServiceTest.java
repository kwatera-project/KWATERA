package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import io.github.kwatera_project.kwatera.billing_service.client.StripeClient;
import io.github.kwatera_project.kwatera.billing_service.exception.WebhookProcessingException;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.math.BigDecimal;
import java.util.Map;
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

  @Mock private PaymentIntent paymentIntent;

  @BeforeEach
  void setup() {
    paymentWebhookService =
        new PaymentWebhookService(settlementService, stripeClient, paymentTransactionService);
    ReflectionTestUtils.setField(paymentWebhookService, "stripeWebhookSecret", "test_secret");
  }

  @Test
  void shouldRegisterPayment_whenCheckoutSessionCompleted() throws Exception {

    // given
    String payload = "payload";
    String signature = "signature";

    Map<String, String> metadata =
        Map.of(
            "settlementId", UUID.randomUUID().toString(),
            "unitId", UUID.randomUUID().toString(),
            "type", "DEPOSIT",
            "description", "Test payment",
            "quantity", "2",
            "unitPrice", "10");

    Session sessionFromEvent = new Session();
    sessionFromEvent.setId("sess_123");

    // Stripe event
    when(stripeClient.constructEvent(payload, signature, "test_secret")).thenReturn(event);

    when(event.getType()).thenReturn("checkout.session.completed");
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    when(deserializer.getObject()).thenReturn(java.util.Optional.of(sessionFromEvent));

    when(stripeClient.retrieveSession("sess_123")).thenReturn(session);
    when(session.getMetadata()).thenReturn(metadata);

    when(session.getId()).thenReturn("sess_123");

    // when
    paymentWebhookService.processWebhook(payload, signature);

    // then
    verify(settlementService, times(1))
        .registerPayment(
            any(UUID.class),
            any(UUID.class),
            eq(SettlementItemType.DEPOSIT),
            eq("Test payment"),
            eq(new BigDecimal("2")),
            eq(new BigDecimal("10")));

    verify(paymentTransactionService)
        .saveSuccessTransaction(
            any(UUID.class),
            any(UUID.class),
            eq(SettlementItemType.DEPOSIT),
            eq("Test payment"),
            eq(new BigDecimal("2")),
            eq(new BigDecimal("10")),
            eq("sess_123"));
  }

  @Test
  void shouldNotRegisterPayment_whenMetadataIsNull() throws Exception {

    String payload = "payload";
    String signature = "signature";

    Session sessionFromEvent = new Session();
    sessionFromEvent.setId("sess_123");

    when(stripeClient.constructEvent(payload, signature, "test_secret")).thenReturn(event);

    when(event.getType()).thenReturn("checkout.session.completed");
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    when(deserializer.getObject()).thenReturn(java.util.Optional.of(sessionFromEvent));

    when(stripeClient.retrieveSession("sess_123")).thenReturn(session);
    when(session.getMetadata()).thenReturn(null);

    // when / then
    assertThrows(
        WebhookProcessingException.class,
        () -> paymentWebhookService.processWebhook(payload, signature));

    verifyNoInteractions(settlementService);
  }

  @Test
  void shouldNotCreateSettlementItem_whenPaymentFailedEvent() throws Exception {

    Map<String, String> metadata =
        Map.of(
            "settlementId", UUID.randomUUID().toString(),
            "unitId", UUID.randomUUID().toString(),
            "type", "DEPOSIT",
            "description", "Test payment",
            "quantity", "2",
            "unitPrice", "10");

    when(stripeClient.constructEvent(any(), any(), any())).thenReturn(event);

    when(event.getType()).thenReturn("payment_intent.payment_failed");

    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    when(deserializer.getObject()).thenReturn(java.util.Optional.of(paymentIntent));

    when(paymentIntent.getMetadata()).thenReturn(metadata);

    when(paymentIntent.getId()).thenReturn("pi_123");

    paymentWebhookService.processWebhook("payload", "sig");

    verifyNoInteractions(settlementService);

    verify(paymentTransactionService)
        .saveFailedTransaction(
            any(UUID.class),
            any(UUID.class),
            eq(SettlementItemType.DEPOSIT),
            eq("Test payment"),
            eq(new BigDecimal("2")),
            eq(new BigDecimal("10")),
            eq("pi_123"),
            eq("UNKNOWN_ERROR"));
  }
}
