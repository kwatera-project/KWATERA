package io.github.kwatera_project.kwatera.review_service.service;

import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.review_service.client.PropertyClient;
import io.github.kwatera_project.kwatera.review_service.client.ReviewAiClient;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewSummaryInput;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class ReviewSummarySchedulerTest {

  @Test
  void retriesOnlyThePropertyAndIncludesCommentsFromUnchangedUnits() {
    ReviewRepository reviews = mock(ReviewRepository.class);
    ReviewAiClient ai = mock(ReviewAiClient.class);
    PropertyClient properties = mock(PropertyClient.class);
    ReviewSummaryScheduler scheduler = new ReviewSummaryScheduler(reviews, ai, properties);
    UUID property = UUID.randomUUID();
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    List<UUID> rooms = List.of(first, second);
    ReviewSummaryInput old = new ReviewSummaryInput(UUID.randomUUID(), "Before", Instant.EPOCH);
    ReviewSummaryInput edited = new ReviewSummaryInput(old.id(), "After", Instant.EPOCH);
    ReviewSummaryInput other = new ReviewSummaryInput(UUID.randomUUID(), "Quiet", Instant.EPOCH);

    when(reviews.findUnitIdsWithReviews()).thenReturn(rooms);
    when(reviews.findSummaryInputsByUnitIds(List.of(first))).thenReturn(List.of(old));
    when(reviews.findSummaryInputsByUnitIds(List.of(second))).thenReturn(List.of(other));
    scheduler.checkForChanges();
    verifyNoInteractions(ai);

    when(reviews.findSummaryInputsByUnitIds(List.of(first))).thenReturn(List.of(edited));
    when(properties.getPropertyIdForUnit(first)).thenReturn(property);
    when(properties.getUnitIdsForProperty(property)).thenReturn(rooms);
    when(reviews.findSummaryInputsByUnitIds(rooms)).thenReturn(List.of(edited, other));
    when(ai.summarize(List.of("After"), "pl")).thenReturn(new ReviewAiClient.SummaryResponse("Room summary", List.of(), List.of(), "qwen"));
    when(ai.summarize(List.of("After", "Quiet"), "pl"))
        .thenThrow(new RestClientException("Unavailable"))
        .thenReturn(new ReviewAiClient.SummaryResponse("Property summary", List.of(), List.of(), "qwen"));

    scheduler.checkForChanges();
    scheduler.checkForChanges();
    scheduler.checkForChanges();

    verify(ai).summarize(List.of("After"), "pl");
    verify(ai, times(2)).summarize(List.of("After", "Quiet"), "pl");
    verifyNoMoreInteractions(ai);
  }
}
