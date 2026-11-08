package io.github.kwatera_project.kwatera.review_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.review_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.review_service.client.ReviewAiClient;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewSummaryRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.datasource.driver-class-name=org.postgresql.Driver")
@Testcontainers(disabledWithoutDocker = true)
class ReviewSummarySchedulerTest {

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired ReviewSummaryScheduler scheduler;
  @Autowired ReviewRepository reviews;
  @Autowired ReviewSummaryRepository summaries;
  @Autowired ObjectMapper mapper;
  @MockitoBean ReviewAiClient ai;
  @MockitoBean PropertyClient properties;

  @Test
  void generatesMissingSummariesThenReusesRefreshesRetriesAndRemovesThem() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    UUID property = UUID.randomUUID();
    var rooms = List.of(first, second);
    when(properties.getPropertyIdForUnit(any())).thenReturn(property);
    when(properties.getUnitIdsForProperty(property)).thenReturn(rooms);
    var old = review(first, "Clean");
    var other = review(second, "Quiet");
    var result = new ReviewAiClient.SummaryResponse("Summary", List.of("Clean"), List.of(), "model");
    when(ai.summarize(anyList(), eq("en"))).thenReturn(result);

    scheduler.checkForChanges();

    var original = summaries.findByPropertyId(property).orElseThrow();
    assertEquals(Set.of(old.getId(), other.getId()), Set.copyOf(original.getReviewIds()));
    assertEquals(result, mapper.readValue(original.getAiSummary(), ReviewAiClient.SummaryResponse.class));
    assertEquals(3, summaries.count());
    verify(ai, times(3)).summarize(anyList(), eq("en"));

    clearInvocations(ai);
    var restarted = new ReviewSummaryScheduler(reviews, ai, properties, summaries, mapper);
    restarted.checkForChanges();
    verifyNoInteractions(ai);

    original.setInputFingerprint(ReviewSummaryFingerprint.calculate(reviews.findSummaryInputsByUnitIds(rooms)));
    summaries.saveAndFlush(original);
    restarted.checkForChanges();
    verify(ai, times(1)).summarize(anyList(), eq("en"));
    original = summaries.findByPropertyId(property).orElseThrow();
    clearInvocations(ai);

    old.setComment("Edited");
    reviews.saveAndFlush(old);
    var editedInputs = reviews.findSummaryInputsByUnitIds(rooms);
    var comments = editedInputs.stream().map(input -> input.comment()).toList();
    when(ai.summarize(comments, "en"))
        .thenThrow(new RestClientException("Unavailable")).thenReturn(result);
    restarted.checkForChanges();
    assertEquals(original.getInputFingerprint(),
        summaries.findByPropertyId(property).orElseThrow().getInputFingerprint());
    restarted.checkForChanges();
    verify(ai).summarize(List.of("Edited"), "en");
    verify(ai, times(2)).summarize(comments, "en");
    var updated = summaries.findByPropertyId(property).orElseThrow();
    assertEquals(original.getId(), updated.getId());
    assertEquals("en:v5:" + ReviewSummaryFingerprint.calculate(editedInputs), updated.getInputFingerprint());
    assertNotEquals(original.getInputFingerprint(), updated.getInputFingerprint());

    clearInvocations(ai);
    reviews.deleteAll();
    restarted.checkForChanges();
    assertEquals(0, summaries.count());
    verifyNoInteractions(ai);
  }

  private Review review(UUID unit, String comment) {
    var review = new Review();
    review.setUnitId(unit);
    review.setReservationId(UUID.randomUUID());
    review.setAuthorId(UUID.randomUUID());
    review.setRating(4);
    review.setComment(comment);
    return reviews.saveAndFlush(review);
  }
}
