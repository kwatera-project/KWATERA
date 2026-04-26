package io.github.kwatera_project.kwatera.auth_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("KWATERA Auth Service API")
                                .version("Stage 2")
                                .description("Authentication and user profile API for the KWATERA Stage 2 prototype."));
    }
}