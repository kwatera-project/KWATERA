package io.github.kwatera_project.kwatera.ai_service.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kwatera.security")
public record InternalSecurityProperties(@NotBlank @Size(min = 32) String internalToken) {
  @Override
  public String toString() {
    return "InternalSecurityProperties[internalToken=REDACTED]";
  }
}
