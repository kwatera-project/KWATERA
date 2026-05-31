package io.github.kwatera_project.kwatera.billing_service.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StripeService {

  private final RestTemplate restTemplate;

  public ReservationDto getReservation(UUID reservationId, String token) {
    String url = "http://reservation-service/api/v1/reservations/" + reservationId;

    HttpHeaders headers = new HttpHeaders();

    String authHeader = token;

    if (!authHeader.startsWith("Bearer ")) {
      authHeader = "Bearer " + authHeader;
    }

    headers.set("Authorization", authHeader);

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<ReservationDto> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, ReservationDto.class);

      return response.getBody();

    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Cannot fetch reservation: " + e.getMessage());
    }
  }

  public String createCheckoutSession(
      Settlement settlement,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice,
      ReservationDto reservationDto)
      throws StripeException {

    UUID reservationId = settlement.getReservationId();
    UUID unitId = reservationDto.getUnitId();
    String recipientEmail = reservationDto.getGuestEmail();

    String currency =
        (reservationDto.getCurrencyInfo() != null
                && reservationDto.getCurrencyInfo().displayCurrency() != null)
            ? reservationDto.getCurrencyInfo().displayCurrency()
            : "pln";

    BigDecimal exchangeRate =
        (reservationDto.getCurrencyInfo() != null
                && reservationDto.getCurrencyInfo().exchangeRate() != null)
            ? reservationDto.getCurrencyInfo().exchangeRate()
            : BigDecimal.ONE;

    if (unitPrice == null || quantity == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "unitPrice and quantity are required");
    }

    BigDecimal totalPrice = unitPrice.multiply(quantity);

    if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reservation price");
    }

    BigDecimal convertedPrice = totalPrice;
    if (!"pln".equalsIgnoreCase(currency)
        && exchangeRate != null
        && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
      convertedPrice = totalPrice.divide(exchangeRate, 2, RoundingMode.HALF_UP);
    }

    long amount =
        convertedPrice
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();

    SessionCreateParams params =
        SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl("http://localhost:5173/reservations/" + reservationId)
            .setCancelUrl("http://localhost:5173/payment-cancel")

            // metadata checkout.session.*
            .putMetadata("reservationId", reservationId.toString())
            .putMetadata("unitId", unitId.toString())
            .putMetadata("settlementId", settlement.getId().toString())
            .putMetadata("type", type.toString())
            .putMetadata("description", description)
            .putMetadata("quantity", quantity.toString())
            .putMetadata("unitPrice", unitPrice.toString())
            .putMetadata("recipientEmail", recipientEmail != null ? recipientEmail : "")

            // metadata payment_intent.*
            .setPaymentIntentData(
                SessionCreateParams.PaymentIntentData.builder()
                    .putMetadata("reservationId", reservationId.toString())
                    .putMetadata("unitId", unitId.toString())
                    .putMetadata("settlementId", settlement.getId().toString())
                    .putMetadata("type", type.toString())
                    .putMetadata("description", description)
                    .putMetadata("quantity", quantity.toString())
                    .putMetadata("unitPrice", unitPrice.toString())
                    .putMetadata("recipientEmail", recipientEmail != null ? recipientEmail : "")
                    .build())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(currency.toLowerCase())
                            .setUnitAmount(amount)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(description)
                                    .build())
                            .build())
                    .build())
            .build();

    Session session = Session.create(params);
    return session.getUrl();
  }
}
