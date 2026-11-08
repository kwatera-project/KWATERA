package io.github.kwatera_project.kwatera.review_service.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kwatera_project.kwatera.review_service.model.ReviewSummary;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.datasource.driver-class-name=org.postgresql.Driver"
    })
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class ReviewSummaryRepositoryTest {

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private ReviewSummaryRepository summaries;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManager entityManager;

  @Test
  @Sql(
      statements = {
        "CREATE TABLE properties (id UUID PRIMARY KEY)",
        "CREATE TABLE units (id UUID PRIMARY KEY)"
      })
  @Sql(
      scripts = {
        "file:../db-migrations/src/main/resources/db/migration/V40__create_review_summaries.sql",
        "file:../db-migrations/src/main/resources/db/migration/V41__add_review_ids_to_review_summaries.sql"
      })
  void storesAndReadsReviewIdsForUnitAndProperty() {
    UUID target = UUID.randomUUID();
    jdbc.update("INSERT INTO units (id) VALUES (?)", target);
    jdbc.update("INSERT INTO properties (id) VALUES (?)", target);

    UUID firstReview = UUID.randomUUID();
    UUID secondReview = UUID.randomUUID();
    ReviewSummary unit = summary(List.of(firstReview));
    unit.setUnitId(target);
    ReviewSummary property = summary(List.of(firstReview, secondReview));
    property.setPropertyId(target);
    summaries.saveAllAndFlush(List.of(unit, property));
    entityManager.clear();

    assertEquals(List.of(firstReview), summaries.findByUnitId(target).orElseThrow().getReviewIds());
    assertEquals(
        List.of(firstReview, secondReview),
        summaries.findByPropertyId(target).orElseThrow().getReviewIds());
  }

  private ReviewSummary summary(List<UUID> ids) {
    ReviewSummary summary = new ReviewSummary();
    summary.setReviewIds(ids);
    summary.setAiSummary("Czysty pokój");
    summary.setInputFingerprint("fingerprint");
    summary.setGenerationTimestamp(Instant.now());
    return summary;
  }
}
