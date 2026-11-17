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
  void generatesAndCachesBothLanguagesThenRefreshesAndRemovesThem() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    UUID property = UUID.randomUUID();
    var rooms = List.of(first, second);
    when(properties.getPropertyIdForUnit(any())).thenReturn(property);
    when(properties.getUnitIdsForProperty(property)).thenReturn(rooms);
    var old = review(first, "Clean");
    var other = review(second, "Quiet");
    var englishResult =
        new ReviewAiClient.SummaryResponse("English summary", List.of("Clean"), List.of(), "model");
    var polishResult =
        new ReviewAiClient.SummaryResponse(
            "Polskie podsumowanie", List.of("Czysto"), List.of(), "model");
    when(ai.summarize(anyList(), eq("en"))).thenReturn(englishResult);
    when(ai.summarize(anyList(), eq("pl"))).thenReturn(polishResult);

    scheduler.checkForChanges();

    var englishProperty = summaries.findByPropertyIdAndLanguage(property, "en").orElseThrow();
    var polishProperty = summaries.findByPropertyIdAndLanguage(property, "pl").orElseThrow();
    String originalEnglishFingerprint = englishProperty.getInputFingerprint();
    assertEquals(Set.of(old.getId(), other.getId()), Set.copyOf(englishProperty.getReviewIds()));
    assertEquals(
        englishResult,
        mapper.readValue(englishProperty.getAiSummary(), ReviewAiClient.SummaryResponse.class));
    assertEquals(
        polishResult,
        mapper.readValue(polishProperty.getAiSummary(), ReviewAiClient.SummaryResponse.class));
    assertEquals(6, summaries.count());
    verify(ai, times(3)).summarize(anyList(), eq("en"));
    verify(ai, times(3)).summarize(anyList(), eq("pl"));

    clearInvocations(ai);
    var restarted = new ReviewSummaryScheduler(reviews, ai, properties, summaries, mapper);
    restarted.checkForChanges();
    verifyNoInteractions(ai);

    polishProperty.setInputFingerprint("stale-fingerprint");
    summaries.saveAndFlush(polishProperty);
    restarted.checkForChanges();
    verify(ai, times(1)).summarize(anyList(), eq("pl"));
    verify(ai, never()).summarize(anyList(), eq("en"));
    polishProperty = summaries.findByPropertyIdAndLanguage(property, "pl").orElseThrow();
    clearInvocations(ai);

    old.setComment("Edited");
    reviews.saveAndFlush(old);
    var editedInputs = reviews.findSummaryInputsByUnitIds(rooms);
    var editedComments = editedInputs.stream().map(input -> input.comment()).toList();
    String previousPolishFingerprint = polishProperty.getInputFingerprint();
    when(ai.summarize(editedComments, "pl"))
        .thenThrow(new RestClientException("Unavailable"))
        .thenReturn(polishResult);
    restarted.checkForChanges();
    verify(ai, times(2)).summarize(anyList(), eq("en"));
    verify(ai, times(2)).summarize(anyList(), eq("pl"));
    var updated = summaries.findByPropertyIdAndLanguage(property, "en").orElseThrow();
    assertEquals(
        "en:v5:" + ReviewSummaryFingerprint.calculate(editedInputs), updated.getInputFingerprint());
    assertEquals(
        previousPolishFingerprint,
        summaries.findByPropertyIdAndLanguage(property, "pl").orElseThrow().getInputFingerprint());

    clearInvocations(ai);
    restarted.checkForChanges();
    verify(ai, times(1)).summarize(editedComments, "pl");
    verify(ai, never()).summarize(anyList(), eq("en"));
    assertEquals(
        "pl:v5:" + ReviewSummaryFingerprint.calculate(editedInputs),
        summaries.findByPropertyIdAndLanguage(property, "pl").orElseThrow().getInputFingerprint());
    assertNotEquals(originalEnglishFingerprint, updated.getInputFingerprint());

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
