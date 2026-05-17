package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import io.github.kwatera_project.kwatera.billing_service.repository.SettlementRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private StripeService stripeService;

  @Mock private SettlementService settlementService;

  @Mock private SettlementRepository settlementRepository;

  @InjectMocks private PaymentService paymentService;

  @Test
  void shouldCreateCheckoutSession() throws Exception {
    UUID reservationId = UUID.randomUUID();

    Settlement settlement = new Settlement();
    settlement.setId(UUID.randomUUID());
    settlement.setReservationId(reservationId);

    when(settlementRepository.findByReservationId(reservationId))
        .thenReturn(Optional.of(settlement));

    when(stripeService.createCheckoutSession(any(), any(), any(), any(), any()))
        .thenReturn("https://checkout.stripe.com/session");

    String url =
        paymentService.createCheckoutSession(
            reservationId,
            "token",
            SettlementItemType.ACCOMMODATION,
            "Test",
            BigDecimal.ONE,
            BigDecimal.TEN);

    assertEquals("https://checkout.stripe.com/session", url);
  }

  @Test
  void shouldCreateSettlementIfNotExists() {
    UUID reservationId = UUID.randomUUID();

    ReservationDto dto = new ReservationDto();
    dto.setTotalPrice(BigDecimal.valueOf(100));

    when(stripeService.getReservation(eq(reservationId), anyString())).thenReturn(dto);

    when(settlementRepository.findByReservationId(reservationId)).thenReturn(Optional.empty());

    Settlement settlement = new Settlement();
    settlement.setReservationId(reservationId);
    settlement.setAccommodationAmount(BigDecimal.valueOf(100));

    when(settlementService.createSettlement(reservationId, BigDecimal.valueOf(100)))
        .thenReturn(settlement);

    Settlement result = paymentService.getOrCreateByReservation(reservationId, "token");

    assertEquals(reservationId, result.getReservationId());
  }
}
