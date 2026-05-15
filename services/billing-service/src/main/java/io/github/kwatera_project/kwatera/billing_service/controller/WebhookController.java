package io.github.kwatera_project.kwatera.billing_service.controller;

import com.stripe.exception.StripeException;
import io.github.kwatera_project.kwatera.billing_service.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class WebhookController {

  private final PaymentWebhookService paymentWebhookService;

  @PostMapping("/webhook")
  public ResponseEntity<String> handleWebhook(
      @RequestBody String payload, @RequestHeader("Stripe-Signature") String signature)
      throws StripeException {
    paymentWebhookService.processWebhook(payload, signature);
    return ResponseEntity.ok("success");
  }
}
