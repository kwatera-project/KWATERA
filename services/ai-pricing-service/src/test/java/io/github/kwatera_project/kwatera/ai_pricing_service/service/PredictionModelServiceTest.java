package io.github.kwatera_project.kwatera.ai_pricing_service.service;

import static io.github.kwatera_project.kwatera.ai_pricing_service.model.UnitType.ENTIRE_RENTAL_UNIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.catboost.CatBoostError;
import ai.catboost.CatBoostModel;
import ai.catboost.CatBoostPredictions;
import io.github.kwatera_project.kwatera.ai_pricing_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.ai_pricing_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.ai_pricing_service.dto.UnitDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PredictionModelServiceTest {

  @Mock PropertyClient propertyClient;

  @Mock CatBoostModel model;

  @Mock CatBoostPredictions predictions;

  PredictionModelService service;

  @BeforeEach
  void setUp() throws CatBoostError {
    service = new PredictionModelService(propertyClient, model);
  }

  @Test
  void shouldPredictPriceCorrectly() throws Exception {
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();

    PropertyDto property = mock(PropertyDto.class);
    UnitDto unit = mock(UnitDto.class);

    when(property.getLatitude()).thenReturn(BigDecimal.valueOf(52.23));
    when(property.getLongitude()).thenReturn(BigDecimal.valueOf(21.01));

    when(unit.getCapacity()).thenReturn(4);
    when(unit.getUnitType()).thenReturn(ENTIRE_RENTAL_UNIT);

    when(propertyClient.getProperty(propertyId)).thenReturn(property);
    when(propertyClient.getUnit(unitId)).thenReturn(unit);

    when(model.predict(any(float[].class), any(String[].class))).thenReturn(predictions);

    when(predictions.get(0, 0)).thenReturn(1234.56);

    BigDecimal result = service.predictPrice(propertyId, unitId, LocalDate.now().toString());

    assertEquals(BigDecimal.valueOf(1234.56), result);
  }

  @Test
  void shouldReturnCorrectRegionNorth() {
    assertEquals("north", PredictionModelService.region(54.0, 20.0));
  }

  @Test
  void shouldReturnCorrectRegionWest() {
    assertEquals("west", PredictionModelService.region(52.0, 16.0));
  }

  @Test
  void shouldReturnCorrectRegionCentral() {
    assertEquals("central", PredictionModelService.region(52.0, 20.0));
  }
}
