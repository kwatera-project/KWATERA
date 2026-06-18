package io.github.kwatera_project.kwatera.billing_service.service;

import com.stripe.exception.StripeException;
import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementItemDto;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementResponseDto;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import jakarta.transaction.Transactional;
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
                    reservationId, reservation.getTotalPrice(), reservation.getGuestEmail());
              } catch (DataIntegrityViolationException _) {
                return settlementRepository.findByReservationId(reservationId).orElseThrow();
              }
            });
  }

  @Transactional
  public String createCheckoutSession(
      UUID reservationId,
      String token,
      io.github.kwatera_project.kwatera.billing_service.dto.CheckoutRequest checkoutRequest)
      throws StripeException {

    Settlement settlement = getOrCreateByReservation(reservationId, token);
    settlement.setInvoiceRequested(checkoutRequest.isInvoiceRequested());
    settlement.setCompanyName(checkoutRequest.getCompanyName());
    settlement.setTaxId(checkoutRequest.getTaxId());
    settlement.setCompanyAddress(checkoutRequest.getCompanyAddress());
    settlementRepository.save(settlement);

    ReservationDto reservationDto = stripeService.getReservation(reservationId, token);

    return stripeService.createCheckoutSession(
        settlement,
        checkoutRequest.getType(),
        checkoutRequest.getDescription(),
        checkoutRequest.getQuantity(),
        checkoutRequest.getUnitPrice(),
        reservationDto);
  }

  public SettlementResponseDto getSettlementWithItems(UUID reservationId, String token) {

    ReservationDto reservation = stripeService.getReservation(reservationId, token);

    return settlementService.getSettlementWithItems(reservation);
  }

  public SettlementItemDto getSettlementItemInfoByType(
      UUID reservationId, SettlementItemType settlementItemType, String token) {

    ReservationDto reservation = stripeService.getReservation(reservationId, token);

    return settlementService.getSettlementItemInfoByType(reservation, settlementItemType);
  }
}
