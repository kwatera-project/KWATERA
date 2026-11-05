package io.github.kwatera_project.kwatera.ai_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  public static final String INTERNAL_TOKEN = "internalToken";

  @Bean
  public OpenAPI aiServiceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("KWATERA Shared AI Service API")
                .version("Stage 2")
                .description(
                    "Internal review summarization and message translation. Inference requires a trusted backend's X-Internal-Token; do not expose the token to browser clients."))
        .components(
            new Components()
                .addSecuritySchemes(
                    INTERNAL_TOKEN,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Internal-Token")
                        .description(
                            "Required shared backend token configured by KWATERA_INTERNAL_TOKEN. Missing or invalid tokens return 403 ACCESS_DENIED.")));
  }
}
