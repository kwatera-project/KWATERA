package io.github.kwatera_project.kwatera.ai_pricing_service;

import ai.catboost.CatBoostModel;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureTestDatabase
class AiPricingServiceApplicationTests {

  @MockitoBean CatBoostModel catBoostModel;
}
