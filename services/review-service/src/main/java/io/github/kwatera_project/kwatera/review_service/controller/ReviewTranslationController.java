package io.github.kwatera_project.kwatera.review_service.controller;

import io.github.kwatera_project.kwatera.review_service.dto.ReviewTranslationRequest;
import io.github.kwatera_project.kwatera.review_service.dto.ReviewTranslationResponse;
import io.github.kwatera_project.kwatera.review_service.service.ReviewTranslationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewTranslationController {

  private final ReviewTranslationService translationService;

  public ReviewTranslationController(ReviewTranslationService translationService) {
    this.translationService = translationService;
  }

  @PostMapping("/{reviewId}/translation")
  public ReviewTranslationResponse translate(
      @PathVariable("reviewId") UUID reviewId,
      @Valid @RequestBody ReviewTranslationRequest request) {

    return translationService.translate(reviewId, request.targetLanguage());
  }
}
