package io.github.kwatera_project.kwatera.reservation_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
  // This configuration enables JPA auditing for automatic timestamping.
}
