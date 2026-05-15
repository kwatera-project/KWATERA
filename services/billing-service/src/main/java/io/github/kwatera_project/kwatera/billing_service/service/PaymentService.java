package io.github.kwatera_project.kwatera.billing_service.service;

import com.stripe.exception.StripeException;
import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementResponseDto;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final StripeService stripeService;
  private final SettlementService settlementService;

  private final SettlementRepository settlementRepository;

  public Settlement getOrCreateByReservation(UUID reservationId, String token) {

    ReservationDto reservation = stripeService.getReservation(reservationId, token);

    return settlementRepository
        .findByReservationId(reservationId)
        .orElseGet(
            () -> {
              try {
                return settlementService.createSettlement(
                    reservationId, reservation.getTotalPrice());
              } catch (DataIntegrityViolationException e) {
                return settlementRepository.findByReservationId(reservationId).orElseThrow();
              }
            });
  }

  @Transactional
  public String createCheckoutSession(
      UUID reservationId,
      String token,
      SettlementItemType type,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice)
      throws StripeException {

    Settlement settlement = getOrCreateByReservation(reservationId, token);

    return stripeService.createCheckoutSession(settlement, type, description, quantity, unitPrice);
  }

  public SettlementResponseDto getSettlementWithItems(UUID reservationId, String token) {

    stripeService.getReservation(reservationId, token);

    return settlementService.getSettlementWithItems(reservationId);
  }
}
