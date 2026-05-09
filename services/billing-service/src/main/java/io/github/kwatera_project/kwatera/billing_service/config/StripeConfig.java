package io.github.kwatera_project.kwatera.billing_service.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeConfig {

  @Value("${stripe.secret-key}")
  private String secretKey;

  @PostConstruct
  public void init() {
    System.out.println("STRIPE KEY = " + secretKey);
    Stripe.apiKey = secretKey;
  }
}
