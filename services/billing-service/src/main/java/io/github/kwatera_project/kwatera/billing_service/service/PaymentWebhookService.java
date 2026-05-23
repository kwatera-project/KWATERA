package io.github.kwatera_project.kwatera.billing_service.service;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import io.github.kwatera_project.kwatera.billing_service.client.StripeClient;
import io.github.kwatera_project.kwatera.billing_service.dto.FailedTransactionCommand;
import io.github.kwatera_project.kwatera.billing_service.dto.PaymentMetadataDto;
import io.github.kwatera_project.kwatera.billing_service.exception.WebhookProcessingException;
import java.util.Optional;
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

    String eventId = event.getId();
    String eventType = event.getType();

    switch (eventType) {
      case "checkout.session.completed" -> handleCheckoutCompleted(event, eventId);

      case "checkout.session.expired" -> handleCheckoutExpired(event, eventId);

      case "payment_intent.payment_failed" -> handlePaymentFailed(event, eventId);

      case "checkout.session.async_payment_failed" -> handleCheckoutFailed(event, eventId);

      default -> {
        // ignore safely
      }
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

  private void handleCheckoutCompleted(Event event, String eventId) throws StripeException {

    Session eventSession = deserializeSession(event);
    Session session = stripeClient.retrieveSession(eventSession.getId());

    PaymentMetadataDto metadata = PaymentMetadataDto.from(session.getMetadata());

    boolean created =
        paymentTransactionService.createProcessingIfNotExists(
            eventId,
            metadata.settlementId(),
            metadata.unitId(),
            metadata.type(),
            metadata.description(),
            metadata.quantity(),
            metadata.unitPrice(),
            session.getId());

    if (!created) {
      return; // already processed
    }

    settlementService.registerPayment(
        metadata.settlementId(),
        metadata.unitId(),
        metadata.type(),
        metadata.description(),
        metadata.quantity(),
        metadata.unitPrice());

    paymentTransactionService.markSuccessIfAllowed(eventId);
  }

  private void handleCheckoutExpired(Event event, String eventId) throws StripeException {

    Session eventSession = deserializeSession(event);
    Session session = stripeClient.retrieveSession(eventSession.getId());

    PaymentMetadataDto metadata = PaymentMetadataDto.from(session.getMetadata());

    paymentTransactionService.markFailed(
        eventId,
        new FailedTransactionCommand(
            metadata.settlementId(),
            metadata.unitId(),
            metadata.type(),
            metadata.description(),
            metadata.quantity(),
            metadata.unitPrice(),
            session.getId(),
            "Checkout session expired"));
  }

  private void handleCheckoutFailed(Event event, String eventId) {

    Session session = deserializeSession(event);

    PaymentMetadataDto metadata = PaymentMetadataDto.from(session.getMetadata());

    paymentTransactionService.markFailed(
        eventId,
        new FailedTransactionCommand(
            metadata.settlementId(),
            metadata.unitId(),
            metadata.type(),
            metadata.description(),
            metadata.quantity(),
            metadata.unitPrice(),
            session.getId(),
            "Checkout payment failed"));
  }

  private void handlePaymentFailed(Event event, String eventId) {

    PaymentIntent paymentIntent = deserializePaymentIntent(event);

    String reason =
        paymentIntent.getLastPaymentError() != null
            ? paymentIntent.getLastPaymentError().getMessage()
            : "UNKNOWN_ERROR";

    PaymentMetadataDto metadata = PaymentMetadataDto.from(paymentIntent.getMetadata());
    String sessionId =
        Optional.ofNullable(paymentIntent.getMetadata())
            .map(m -> m.get("checkoutSessionId"))
            .orElse(paymentIntent.getId());

    paymentTransactionService.markFailed(
        eventId,
        new FailedTransactionCommand(
            metadata.settlementId(),
            metadata.unitId(),
            metadata.type(),
            metadata.description(),
            metadata.quantity(),
            metadata.unitPrice(),
            sessionId,
            reason));
  }
}
