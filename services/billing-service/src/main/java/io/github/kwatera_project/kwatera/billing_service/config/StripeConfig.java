package io.github.kwatera_project.kwatera.billing_service.config;

import com.stripe.Stripe;
import org.apache.logging.log4j.internal.annotation.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@SuppressFBWarnings(
    value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
    justification = "Required by Stripe SDK")
public class StripeConfig {

  public StripeConfig(@Value("${stripe.secret-key}") String secretKey) {
    Stripe.apiKey = secretKey;
  }
}
