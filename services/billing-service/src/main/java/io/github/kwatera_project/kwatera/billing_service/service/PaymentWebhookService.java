package io.github.kwatera_project.kwatera.billing_service.service;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import io.github.kwatera_project.kwatera.billing_service.client.StripeClient;
import io.github.kwatera_project.kwatera.billing_service.exception.WebhookProcessingException;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

  private final SettlementService settlementService;

  private final StripeClient stripeClient;

  @Value("${stripe.webhook.secret}")
  private String stripeWebhookSecret;

  public void processWebhook(String payload, String signature) throws StripeException {

    Event event = stripeClient.constructEvent(payload, signature, stripeWebhookSecret);

    if ("checkout.session.completed".equals(event.getType())) {

      EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
      Session sessionData = null;

      sessionData =
          dataObjectDeserializer
              .getObject()
              .map(Session.class::cast)
              .orElseGet(
                  () -> {
                    try {
                      return (Session) dataObjectDeserializer.deserializeUnsafe();
                    } catch (EventDataObjectDeserializationException e) {
                      throw new WebhookProcessingException(
                          "Failed to deserialize Stripe event data object", e);
                    }
                  });

      Session session = stripeClient.retrieveSession(sessionData.getId());

      Map<String, String> metadata = session.getMetadata();
      if (metadata == null) {
        throw new WebhookProcessingException("Metadata is missing entirely");
      }

      String settlementIdStr = session.getMetadata().get("settlementId");
      if (settlementIdStr == null || settlementIdStr.isEmpty()) {
        throw new WebhookProcessingException("Missing settlementId in metadata");
      }

      UUID settlementId = UUID.fromString(settlementIdStr);
      SettlementItemType type = SettlementItemType.valueOf(session.getMetadata().get("type"));
      String description = session.getMetadata().get("description");

      BigDecimal quantity = parseBigDecimal(metadata.get("quantity"));
      BigDecimal unitPrice = parseBigDecimal(metadata.get("unitPrice"));

      settlementService.registerPayment(settlementId, type, description, quantity, unitPrice);
    }
  }

  private BigDecimal parseBigDecimal(String val) {
    try {
      return (val != null) ? new BigDecimal(val) : BigDecimal.ZERO;
    } catch (NumberFormatException _) {
      return BigDecimal.ZERO;
    }
  }
}
