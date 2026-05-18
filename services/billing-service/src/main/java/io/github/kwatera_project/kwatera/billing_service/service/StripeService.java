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

  ReservationDto getReservation(UUID reservationId, String token) {
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
      BigDecimal unitPrice)
      throws StripeException {

    UUID reservationId = settlement.getReservationId();

    if (unitPrice == null || quantity == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "unitPrice and quantity are required");
    }

    BigDecimal totalPrice = unitPrice.multiply(quantity);

    if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reservation price");
    }

    long amount =
        totalPrice
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();

    SessionCreateParams params =
        SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl("http://localhost:5173/reservations/" + reservationId)
            .setCancelUrl("http://localhost:5173/payment-cancel")
            .putMetadata("reservationId", reservationId.toString())
            .putMetadata("settlementId", settlement.getId().toString())
            .putMetadata("type", type.toString())
            .putMetadata("description", description)
            .putMetadata("quantity", quantity.toString())
            .putMetadata("unitPrice", unitPrice.toString())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("pln")
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
