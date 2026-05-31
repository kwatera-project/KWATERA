package io.github.kwatera_project.kwatera.billing_service.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import io.github.kwatera_project.kwatera.billing_service.client.OcrClient;
import io.github.kwatera_project.kwatera.billing_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.billing_service.event.SettlementEventPublisher;
import io.github.kwatera_project.kwatera.billing_service.service.EmailNotificationService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

// Connect Cucumber with Spring Boot test context
@CucumberContextConfiguration
// Run BDD tests with full Spring context
@SpringBootTest(
    properties = {
      "spring.cloud.discovery.enabled=false",
      "eureka.client.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.show-sql=false"
    })
public class CucumberSpringConfig {
  // PostgreSQL database used only for BDD test
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("billing_bdd_test")
          .withUsername("test")
          .withPassword("test");

  // Start container before Spring tries to read database connection data
  static {
    postgres.start();
  }

  // Pass PostgreSQL container connection data to Spring datasource configuration
  @DynamicPropertySource
  static void configurePostgres(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
  }

  @MockitoBean protected SettlementEventPublisher settlementEventPublisher;
  @MockitoBean protected EmailNotificationService emailNotificationService;
  @MockitoBean protected OcrClient ocrClient;
  @MockitoBean protected PropertyClient propertyClient;
}
