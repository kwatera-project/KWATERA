package io.github.kwatera_project.kwatera.billing_service.service;

import static org.mockito.Mockito.*;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import io.github.kwatera_project.kwatera.billing_service.client.StripeClient;
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

  @Mock SettlementService settlementService;
  @Mock StripeClient stripeClient;

  @InjectMocks PaymentWebhookService service;

  @BeforeEach
  void setup() {
    service = new PaymentWebhookService(settlementService, stripeClient);
    ReflectionTestUtils.setField(service, "stripeWebhookSecret", "test_secret");
  }

  @Test
  void shouldProcessWebhook() throws Exception {

    String payload = "payload";
    String sig = "sig";

    Event event = mock(Event.class);
    Session session = mock(Session.class);

    when(stripeClient.constructEvent(payload, sig, "test_secret")).thenReturn(event);

    when(event.getType()).thenReturn("checkout.session.completed");

    EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);

    when(deserializer.getObject()).thenReturn(Optional.of(session));
    when(session.getId()).thenReturn("sess_123");

    when(stripeClient.retrieveSession("sess_123")).thenReturn(session);

    Map<String, String> metadata =
        Map.of(
            "settlementId", UUID.randomUUID().toString(),
            "type", "ACCOMMODATION",
            "description", "test",
            "quantity", "1",
            "unitPrice", "100");

    when(session.getMetadata()).thenReturn(metadata);

    service.processWebhook(payload, sig);

    verify(settlementService).registerPayment(any(), any(), any(), any(), any());
  }
}
