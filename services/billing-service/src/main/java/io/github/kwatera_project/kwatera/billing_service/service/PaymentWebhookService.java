package io.github.kwatera_project.kwatera.billing_service.service;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import io.github.kwatera_project.kwatera.billing_service.client.StripeClient;
import io.github.kwatera_project.kwatera.billing_service.dto.PaymentMetadataDto;
import io.github.kwatera_project.kwatera.billing_service.exception.WebhookProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

  private final SettlementService settlementService;

  private final StripeClient stripeClient;

  private final PaymentTransactionService paymentTransactionService;

  @Value("${stripe.webhook.secret}")
  private String stripeWebhookSecret;

  public void processWebhook(String payload, String signature) throws StripeException {

    Event event = stripeClient.constructEvent(payload, signature, stripeWebhookSecret);

    String eventType = event.getType();

    if ("checkout.session.completed".equals(eventType)) {
      handleCheckoutCompleted(event);
    }

    if ("checkout.session.expired".equals(eventType)) {
      handleCheckoutExpired(event);
    }

    if ("payment_intent.payment_failed".equals(eventType)) {
      handlePaymentFailed(event);
    }
  }

  private Session deserializeSession(Event event) {

    EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

    return deserializer
        .getObject()
        .map(Session.class::cast)
        .orElseGet(
            () -> {
              try {
                return (Session) deserializer.deserializeUnsafe();
              } catch (EventDataObjectDeserializationException e) {
                throw new WebhookProcessingException("Failed to deserialize session", e);
              }
            });
  }

  private PaymentIntent deserializePaymentIntent(Event event) {

    EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

    return deserializer
        .getObject()
        .map(PaymentIntent.class::cast)
        .orElseGet(
            () -> {
              try {
                return (PaymentIntent) deserializer.deserializeUnsafe();
              } catch (EventDataObjectDeserializationException e) {
                throw new WebhookProcessingException("Failed to deserialize payment intent", e);
              }
            });
  }

  private void handleCheckoutCompleted(Event event) throws StripeException {

    Session eventSession = deserializeSession(event);

    Session session = stripeClient.retrieveSession(eventSession.getId());

    PaymentMetadataDto metadata = PaymentMetadataDto.from(session.getMetadata());
    String sessionId = session.getId();

    try {
      settlementService.registerPayment(
          metadata.settlementId(),
          metadata.unitId(),
          metadata.type(),
          metadata.description(),
          metadata.quantity(),
          metadata.unitPrice());

      paymentTransactionService.saveSuccessTransaction(
          metadata.settlementId(),
          metadata.unitId(),
          metadata.type(),
          metadata.description(),
          metadata.quantity(),
          metadata.unitPrice(),
          sessionId);
    } catch (Exception e) {

      paymentTransactionService.saveFailedTransaction(
          metadata.settlementId(),
          metadata.unitId(),
          metadata.type(),
          metadata.description(),
          metadata.quantity(),
          metadata.unitPrice(),
          sessionId,
          e.getMessage());

      throw e;
    }
  }

  private void handleCheckoutExpired(Event event) throws StripeException {

    Session eventSession = deserializeSession(event);

    Session session = stripeClient.retrieveSession(eventSession.getId());

    PaymentMetadataDto metadata = PaymentMetadataDto.from(session.getMetadata());
    String sessionId = session.getId();

    paymentTransactionService.saveFailedTransaction(
        metadata.settlementId(),
        metadata.unitId(),
        metadata.type(),
        metadata.description(),
        metadata.quantity(),
        metadata.unitPrice(),
        sessionId,
        "Checkout session expired");
  }

  private void handlePaymentFailed(Event event) throws StripeException {

    PaymentIntent paymentIntent = deserializePaymentIntent(event);

    String reason =
        paymentIntent.getLastPaymentError() != null
            ? paymentIntent.getLastPaymentError().getMessage()
            : "UNKNOWN_ERROR";

    PaymentMetadataDto metadata = PaymentMetadataDto.from(paymentIntent.getMetadata());
    String sessionId = paymentIntent.getId();

    paymentTransactionService.saveFailedTransaction(
        metadata.settlementId(),
        metadata.unitId(),
        metadata.type(),
        metadata.description(),
        metadata.quantity(),
        metadata.unitPrice(),
        sessionId,
        reason);
  }
}
