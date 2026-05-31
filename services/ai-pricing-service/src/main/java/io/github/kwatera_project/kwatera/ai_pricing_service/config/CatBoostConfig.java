package io.github.kwatera_project.kwatera.ai_pricing_service.config;

import ai.catboost.CatBoostError;
import ai.catboost.CatBoostModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatBoostConfig {

  @Bean
  public CatBoostModel catBoostModel() throws CatBoostError {
    return CatBoostModel.loadModel("/app/catboost_model_v1.cbm");
  }
}
