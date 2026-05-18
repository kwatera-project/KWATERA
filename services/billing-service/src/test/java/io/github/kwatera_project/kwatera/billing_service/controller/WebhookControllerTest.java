package io.github.kwatera_project.kwatera.billing_service.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.kwatera_project.kwatera.billing_service.service.PaymentWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PaymentWebhookService paymentWebhookService;

  @Test
  void shouldHandleWebhookSuccessfully() throws Exception {
    String payload = "{\"type\":\"checkout.session.completed\"}";
    String signature = "test_signature";

    mockMvc
        .perform(
            post("/api/billing/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("Stripe-Signature", signature))
        .andExpect(status().isOk())
        .andExpect(content().string("success"));

    verify(paymentWebhookService).processWebhook(payload, signature);
  }
}
