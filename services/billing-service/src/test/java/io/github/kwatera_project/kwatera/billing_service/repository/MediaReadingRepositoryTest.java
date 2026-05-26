package io.github.kwatera_project.kwatera.billing_service.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.github.kwatera_project.kwatera.billing_service.model.MediaReading;
import io.github.kwatera_project.kwatera.billing_service.model.UtilityType;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("integration-test")
@Testcontainers
@Transactional
public class MediaReadingRepositoryTest {

  @Autowired private MediaReadingRepository repository;

  @Autowired private EntityManager entityManager;

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

    registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
  }

  @Test
  void shouldCalculateConsumptionDifferenceAndCost() {
    MediaReading reading = new MediaReading();

    reading.setSettlementId(UUID.randomUUID());
    reading.setUtilityType(UtilityType.WATER);

    reading.setInitialReading(new BigDecimal("100.000"));
    reading.setInitialConfidenceScore(new BigDecimal("0.95"));

    reading.setFinalReading(new BigDecimal("150.000"));
    reading.setFinalConfidenceScore(new BigDecimal("0.98"));

    reading.setUnitPrice(new BigDecimal("2.50"));

    MediaReading saved = repository.save(reading);

    entityManager.flush();

    entityManager.clear();

    MediaReading refreshed = repository.findById(saved.getId()).orElseThrow();

    assertThat(refreshed.getConsumptionDifference()).isEqualByComparingTo("50.000000");
    assertThat(refreshed.getCalculatedCost()).isEqualByComparingTo("125.00");
  }

  @Test
  void shouldReturnNullCalculatedFieldsWhenFinalReadingIsNull() {
    MediaReading reading = new MediaReading();

    reading.setSettlementId(UUID.randomUUID());
    reading.setUtilityType(UtilityType.WATER);

    reading.setInitialReading(new BigDecimal("200.000"));
    reading.setInitialConfidenceScore(new BigDecimal("0.90"));

    reading.setUnitPrice(new BigDecimal("3.00"));

    MediaReading saved = repository.save(reading);

    entityManager.flush();
    entityManager.clear();

    MediaReading refreshed = repository.findById(saved.getId()).orElseThrow();

    assertThat(refreshed.getConsumptionDifference()).isNull();
    assertThat(refreshed.getCalculatedCost()).isNull();
  }

  @Test
  void shouldFailWhenFinalReadingIsLowerThanInitial() {
    MediaReading reading = new MediaReading();

    reading.setSettlementId(UUID.randomUUID());
    reading.setUtilityType(UtilityType.ELECTRICITY);

    reading.setInitialReading(new BigDecimal("500"));
    reading.setInitialConfidenceScore(new BigDecimal("0.90"));

    reading.setFinalReading(new BigDecimal("400"));
    reading.setFinalConfidenceScore(new BigDecimal("0.90"));

    reading.setUnitPrice(new BigDecimal("1.20"));

    assertThatThrownBy(() -> repository.saveAndFlush(reading))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
