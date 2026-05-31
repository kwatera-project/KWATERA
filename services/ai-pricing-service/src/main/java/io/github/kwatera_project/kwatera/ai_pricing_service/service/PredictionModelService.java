package io.github.kwatera_project.kwatera.ai_pricing_service.service;

import ai.catboost.CatBoostError;
import ai.catboost.CatBoostModel;
import ai.catboost.CatBoostPredictions;
import io.github.kwatera_project.kwatera.ai_pricing_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.ai_pricing_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.ai_pricing_service.dto.UnitDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PredictionModelService {

  private final CatBoostModel model;
  private final PropertyClient propertyClient;

  public PredictionModelService(PropertyClient propertyClient, CatBoostModel model)
      throws CatBoostError {
    this.propertyClient = propertyClient;
    this.model = model;
  }

  public static String region(double lat, double lon) {
    if (lat > 53) return "north";
    if (lon < 17) return "west";
    return "central";
  }

  public BigDecimal predictPrice(UUID propertyId, UUID unitId, String currentDate)
      throws CatBoostError {

    PropertyDto property = propertyClient.getProperty(propertyId);
    UnitDto unit = propertyClient.getUnit(unitId);

    double latitude = property.getLatitude().doubleValue();
    double longitude = property.getLongitude().doubleValue();
    int month = LocalDate.parse(currentDate).getMonthValue();

    double centerDistance =
        Math.sqrt(Math.pow(latitude - 52.2297, 2) + Math.pow(longitude - 21.0122, 2));

    String region = region(latitude, longitude);

    String[] categoricalFeatures = new String[] {unit.getUnitType().getValue(), region};

    float[] numericFeatures =
        new float[] {
          (float) latitude,
          (float) longitude,
          (float) unit.getCapacity(),
          (float) month,
          (float) centerDistance
        };

    CatBoostPredictions result = model.predict(numericFeatures, categoricalFeatures);

    double logPrediction = result.get(0, 0);
    double realPrediction = Math.expm1(logPrediction);

    if (realPrediction < 0) {
      realPrediction = 0;
    }

    return BigDecimal.valueOf(realPrediction).setScale(2, RoundingMode.HALF_UP);
  }
}
