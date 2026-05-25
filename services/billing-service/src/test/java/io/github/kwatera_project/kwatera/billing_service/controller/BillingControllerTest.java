package io.github.kwatera_project.kwatera.billing_service.controller;

import static io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType.ACCOMMODATION;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.billing_service.dto.CheckoutRequest;
import io.github.kwatera_project.kwatera.billing_service.dto.SettlementResponseDto;
import io.github.kwatera_project.kwatera.billing_service.service.PaymentService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BillingControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PaymentService paymentService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void shouldCreateCheckout() throws Exception {
    UUID reservationId = UUID.randomUUID();

    CheckoutRequest request = new CheckoutRequest();
    request.setType(ACCOMMODATION);
    request.setDescription("test");
    request.setQuantity(new BigDecimal(2));
    request.setUnitPrice(new BigDecimal(100));

    when(paymentService.createCheckoutSession(
            eq(reservationId),
            eq("Bearer token"),
            eq(ACCOMMODATION),
            eq("test"),
            any(BigDecimal.class),
            any(BigDecimal.class)))
        .thenReturn("https://stripe.checkout/session");

    mockMvc
        .perform(
            post("/api/billing/checkout/" + reservationId)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("https://stripe.checkout/session"));
  }

  @Test
  void shouldReturnSettlement() throws Exception {
    UUID reservationId = UUID.randomUUID();

    SettlementResponseDto dto = mock(SettlementResponseDto.class);

    when(paymentService.getSettlementWithItems(reservationId, "Bearer token", "PLN")).thenReturn(dto);

    mockMvc
        .perform(
            get("/api/billing/settlements/" + reservationId)
                .header("Authorization", "Bearer token"))
        .andExpect(status().isOk());
  }
}
