package io.github.kwatera_project.kwatera.property_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@ConditionalOnProperty(
    name = "app.jpa.auditing.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class JpaConfig {}
