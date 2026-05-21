package io.github.kwatera_project.kwatera.billing_service.client;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Component;

@Component
public class StripeClient {

  public Event constructEvent(String payload, String signature, String secret)
      throws StripeException {
    return Webhook.constructEvent(payload, signature, secret);
  }

  public Session retrieveSession(String id) throws StripeException {
    return Session.retrieve(id);
  }
}
