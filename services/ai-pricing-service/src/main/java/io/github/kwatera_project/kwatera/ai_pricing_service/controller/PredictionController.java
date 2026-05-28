package io.github.kwatera_project.kwatera.ai_pricing_service.controller;

import ai.catboost.CatBoostError;
import io.github.kwatera_project.kwatera.ai_pricing_service.service.PredictionModelService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
public class PredictionController {

  private final PredictionModelService predictionService;

  @GetMapping("/price")
  public ResponseEntity<BigDecimal> getPredictedPrice(
      @RequestParam("propertyId") UUID propertyId,
      @RequestParam("unitId") UUID unitId,
      @RequestParam(value = "date", required = false) String date)
      throws CatBoostError {

    String dateToUse = (date != null) ? date : LocalDate.now().toString();

    BigDecimal prediction = predictionService.predictPrice(propertyId, unitId, dateToUse);

    return ResponseEntity.ok(prediction);
  }
}
