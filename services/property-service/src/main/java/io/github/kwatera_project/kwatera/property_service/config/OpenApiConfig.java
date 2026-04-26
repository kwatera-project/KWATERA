package io.github.kwatera_project.kwatera.property_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI propertyServiceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("KWATERA Property Service API")
                .version("Stage 2")
                .description(
                    "Property, unit, and image catalogue API for the KWATERA Stage 2 prototype."));
  }
}
