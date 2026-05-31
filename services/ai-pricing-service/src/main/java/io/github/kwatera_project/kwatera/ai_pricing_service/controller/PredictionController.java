package io.github.kwatera_project.kwatera.ai_pricing_service.controller;

import ai.catboost.CatBoostError;
import io.github.kwatera_project.kwatera.ai_pricing_service.service.PredictionModelService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
public class PredictionController {

  private final PredictionModelService predictionService;

    @GetMapping("/price/property/{propertyId}/unit/{unitId}/date/{date}")
    public ResponseEntity<BigDecimal> getPredictedPriceWithDate(
            @PathVariable("propertyId") UUID propertyId,
            @PathVariable("unitId") UUID unitId,
            @PathVariable("date") String date) throws CatBoostError {

        return handlePrediction(propertyId, unitId, date);
    }

    @GetMapping("/price/property/{propertyId}/unit/{unitId}")
    public ResponseEntity<BigDecimal> getPredictedPriceWithoutDate(
            @PathVariable("propertyId") UUID propertyId,
            @PathVariable("unitId") UUID unitId) throws CatBoostError {

        return handlePrediction(propertyId, unitId, null);
    }

    private ResponseEntity<BigDecimal> handlePrediction(UUID propertyId, UUID unitId, String date)
            throws CatBoostError {

        String dateToUse = (date != null) ? date : LocalDate.now().toString();
        BigDecimal prediction = predictionService.predictPrice(propertyId, unitId, dateToUse);
        return ResponseEntity.ok(prediction);
    }
}

