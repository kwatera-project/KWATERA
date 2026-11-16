package io.github.kwatera_project.kwatera.review_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.github.kwatera_project.kwatera.review_service.client.ReviewTranslationClient;
import io.github.kwatera_project.kwatera.review_service.model.Review;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReviewTranslationServiceTest {

  @Mock private ReviewRepository reviewRepository;
  @Mock private ReviewTranslationClient translationClient;

  private ReviewTranslationService translationService;
  private UUID reviewId;
  private Review review;

  @BeforeEach
  void setUp() {
    translationService = new ReviewTranslationService(reviewRepository, translationClient);
    reviewId = UUID.randomUUID();
    review = new Review();
    review.setId(reviewId);
    review.setComment("Great room.");
  }

  @Test
  void translatesCommentWithoutSavingChangesToReview() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(translationClient.translate("Great room.", "pl"))
        .thenReturn(new ReviewTranslationClient.TranslationResult("Świetny pokój.", "EN", false));

    var result = translationService.translate(reviewId, "pl");

    assertEquals("Świetny pokój.", result.translatedText());
    assertEquals("pl", result.targetLanguage());
    assertEquals("EN", result.sourceLanguage());
    assertEquals(false, result.alreadyInTargetLanguage());
    assertEquals("Great room.", review.getComment());
    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  void returnsNotFoundWhenReviewDoesNotExist() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

    var exception =
        assertThrows(
            ResponseStatusException.class, () -> translationService.translate(reviewId, "pl"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    verifyNoInteractions(translationClient);
  }

  @Test
  void rejectsReviewWithoutComment() {
    review.setComment("  ");
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    var exception =
        assertThrows(
            ResponseStatusException.class, () -> translationService.translate(reviewId, "pl"));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    verifyNoInteractions(translationClient);
  }

  @Test
  void mapsTranslationProviderFailureToBadGateway() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(translationClient.translate("Great room.", "pl"))
        .thenThrow(new RestClientException("AI is unavailable"));

    var exception =
        assertThrows(
            ResponseStatusException.class, () -> translationService.translate(reviewId, "pl"));

    assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
  }
}
