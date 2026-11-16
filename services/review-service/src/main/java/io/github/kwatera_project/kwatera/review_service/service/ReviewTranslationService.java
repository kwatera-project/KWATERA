package io.github.kwatera_project.kwatera.review_service.service;

import io.github.kwatera_project.kwatera.review_service.client.ReviewTranslationClient;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewTranslationResponse;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewTranslationService {

  private final ReviewRepository reviewRepository;
  private final ReviewTranslationClient translationClient;

  public ReviewTranslationService(
      ReviewRepository reviewRepository,
      ReviewTranslationClient translationClient) {
    this.reviewRepository = reviewRepository;
    this.translationClient = translationClient;
  }

  public ReviewTranslationResponse translate(UUID reviewId, String targetLanguage) {

    var review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));

    String comment = review.getComment();
    if (comment == null || comment.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "This review has no comment to translate");
    }

    try {
      String translatedText = translationClient.translate(comment, targetLanguage);

      return new ReviewTranslationResponse(translatedText, targetLanguage);
    } catch (RestClientException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY,
          "Review translation is temporarily unavailable",
          exception);
    }
  }
}
