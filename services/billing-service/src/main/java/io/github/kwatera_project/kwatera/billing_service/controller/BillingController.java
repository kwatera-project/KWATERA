package io.github.kwatera_project.kwatera.billing_service.controller;

import com.stripe.exception.StripeException;
import io.github.kwatera_project.kwatera.billing_service.dto.CheckoutRequest;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementItemDto;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementResponseDto;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

  private final PaymentService paymentService;

  @PostMapping("/checkout/{reservationId}")
  public ResponseEntity<String> createCheckout(
      @PathVariable("reservationId") UUID reservationId,
      HttpServletRequest request,
      @RequestBody CheckoutRequest checkoutRequest)
      throws StripeException {

    String token = request.getHeader("Authorization");

    String checkoutUrl =
        paymentService.createCheckoutSession(
            reservationId,
            token,
            checkoutRequest.getType(),
            checkoutRequest.getDescription(),
            checkoutRequest.getQuantity(),
            checkoutRequest.getUnitPrice());

    return ResponseEntity.ok(checkoutUrl);
  }

  @GetMapping("settlements/{reservationId}")
  public ResponseEntity<SettlementResponseDto> getSettlementAndSettlementItems(
      @PathVariable("reservationId") UUID reservationId, HttpServletRequest request) {
    String token = request.getHeader("Authorization");

    SettlementResponseDto response = paymentService.getSettlementWithItems(reservationId, token);

    return ResponseEntity.ok(response);
  }

  @GetMapping("settlements/{reservationId}/{settlementItemType}")
  public ResponseEntity<SettlementItemDto> getSettlementItemInfoByType(
      @PathVariable("reservationId") UUID reservationId,
      @PathVariable("settlementItemType") SettlementItemType settlementItemType,
      HttpServletRequest request) {
    String token = request.getHeader("Authorization");

    SettlementItemDto response =
        paymentService.getSettlementItemInfoByType(reservationId, settlementItemType, token);

    return ResponseEntity.ok(response);
  }
}
