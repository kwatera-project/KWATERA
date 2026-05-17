package io.github.kwatera_project.kwatera.billing_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.kwatera_project.kwatera.billing_service.dto.ReservationDto;
import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {
  @Mock private RestTemplate restTemplate;

  @InjectMocks private StripeService stripeService;

  @Test
  void shouldReturnReservationWhenReservationExists() {
    UUID reservationId = UUID.randomUUID();
    String token = "test-token";

    ReservationDto dto = new ReservationDto();
    dto.setId(reservationId);

    ResponseEntity<ReservationDto> response = ResponseEntity.ok(dto);

    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ReservationDto.class)))
        .thenReturn(response);

    ReservationDto result = stripeService.getReservation(reservationId, token);

    assertNotNull(result);
    assertEquals(reservationId, result.getId());
  }

  @Test
  void shouldThrowBadGatewayWhenReservationServiceFails() {
    UUID reservationId = UUID.randomUUID();

    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ReservationDto.class)))
        .thenThrow(new RuntimeException("service down"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> stripeService.getReservation(reservationId, "token"));

    assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
  }

  @Test
  void shouldCreateCheckoutSession() throws Exception {
    Settlement settlement = new Settlement();
    settlement.setId(UUID.randomUUID());
    settlement.setReservationId(UUID.randomUUID());

    Session mockedSession = mock(Session.class);
    when(mockedSession.getUrl()).thenReturn("https://checkout.stripe.com/test");

    try (MockedStatic<Session> mocked = mockStatic(Session.class)) {
      mocked.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockedSession);

      String url =
          stripeService.createCheckoutSession(
              settlement,
              SettlementItemType.ACCOMMODATION,
              "Accommodation fee",
              BigDecimal.ONE,
              BigDecimal.valueOf(100));

      assertEquals("https://checkout.stripe.com/test", url);
    }
  }

  @Test
  void shouldThrowWhenQuantityIsNull() {
    Settlement settlement = new Settlement();
    settlement.setReservationId(UUID.randomUUID());

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                stripeService.createCheckoutSession(
                    settlement,
                    SettlementItemType.ACCOMMODATION,
                    "Accommodation fee",
                    null,
                    BigDecimal.TEN));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldThrowWhenTotalPriceIsNegative() {
    Settlement settlement = new Settlement();
    settlement.setReservationId(UUID.randomUUID());

    BigDecimal negativeAmount = BigDecimal.valueOf(-1);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () ->
                stripeService.createCheckoutSession(
                    settlement,
                    SettlementItemType.ACCOMMODATION,
                    "Accommodation fee",
                    negativeAmount,
                    BigDecimal.TEN));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }
}
