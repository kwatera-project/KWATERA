package io.github.kwatera_project.kwatera.ai_service.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("ollama")
public class OllamaHealthIndicator implements HealthIndicator {
  private final OllamaApi api;
  private final AiProperties properties;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed diagnostic client")
  public OllamaHealthIndicator(
      @Qualifier("readinessOllamaApi") OllamaApi api, AiProperties properties) {
    this.api = api;
    this.properties = properties;
  }

  @Override
  public Health health() {
    try {
      boolean present =
          api.listModels().models().stream()
              .anyMatch(model -> properties.model().equals(model.name()));
      return present ? Health.up().build() : Health.down().build();
    } catch (RuntimeException exception) {
      // Never attach backend exception messages or model responses to public diagnostics.
      return Health.down().build();
    }
  }
}
