package io.github.kwatera_project.kwatera.review_service.config;

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
                .title("KWATERA Review Service API")
                .version("Stage 2")
                .description(
                    "Review API for the KWATERA prototype."));
  }
}
