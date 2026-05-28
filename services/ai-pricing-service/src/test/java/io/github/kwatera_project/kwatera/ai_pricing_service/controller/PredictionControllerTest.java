package io.github.kwatera_project.kwatera.ai_pricing_service.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.kwatera_project.kwatera.ai_pricing_service.service.PredictionModelService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PredictionController.class)
class PredictionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PredictionModelService service;

  @Test
  void shouldReturnPredictedPrice() throws Exception {
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(service.predictPrice(propertyId, unitId, "2025-01-01"))
        .thenReturn(BigDecimal.valueOf(999.99));

    mockMvc
        .perform(
            get("/api/predict/price")
                .param("propertyId", propertyId.toString())
                .param("unitId", unitId.toString())
                .param("date", "2025-01-01"))
        .andExpect(status().isOk())
        .andExpect(content().string("999.99"));
  }

  @Test
  void shouldUseCurrentDateWhenMissing() throws Exception {
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    when(service.predictPrice(eq(propertyId), eq(unitId), eq("2025-01-01")))
        .thenReturn(BigDecimal.valueOf(1000));

    mockMvc
        .perform(
            get("/api/predict/price")
                .param("propertyId", propertyId.toString())
                .param("unitId", unitId.toString()))
        .andExpect(status().isOk());
  }
}
