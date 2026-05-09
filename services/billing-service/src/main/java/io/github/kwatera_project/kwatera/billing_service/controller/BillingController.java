package io.github.kwatera_project.kwatera.billing_service.controller;

import com.stripe.exception.StripeException;
import io.github.kwatera_project.kwatera.billing_service.service.StripeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

  private final StripeService stripeService;

  @PostMapping("/checkout/{reservationId}")
  public ResponseEntity<String> createCheckout(
      @PathVariable("reservationId") UUID reservationId, HttpServletRequest request)
      throws StripeException {

    String token = request.getHeader("Authorization");

    String checkoutUrl = stripeService.createCheckoutSession(reservationId, token);
    return ResponseEntity.ok(checkoutUrl);
  }
}
