package io.github.kwatera_project.kwatera.reservation_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI reservationServiceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("KWATERA Reservation Service API")
                .version("Stage 2")
                .description(
                    "Availability and reservation overview API for the KWATERA Stage 2 prototype."));
  }
}
