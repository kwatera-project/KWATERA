package io.github.kwatera_project.kwatera.ai_service.validation;

import io.github.kwatera_project.kwatera.ai_service.config.AiProperties;
import io.github.kwatera_project.kwatera.ai_service.dto.SummarizeRequest;
import io.github.kwatera_project.kwatera.ai_service.dto.TranslateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;

public class AiInputLimitsValidator implements ConstraintValidator<WithinAiLimits, Object> {
  private final AiProperties properties;

  public AiInputLimitsValidator(AiProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean isValid(Object value, ConstraintValidatorContext context) {
    if (value instanceof SummarizeRequest request && request.reviews() != null) {
      return request.reviews().size() <= properties.maxReviews()
          && request.reviews().stream()
              .filter(Objects::nonNull)
              .allMatch(text -> text.length() <= properties.maxReviewCharacters())
          && request.reviews().stream().filter(Objects::nonNull).mapToLong(String::length).sum()
              <= properties.maxTotalReviewCharacters();
    }
    if (value instanceof TranslateRequest request && request.text() != null) {
      return request.text().length() <= properties.maxTranslationCharacters();
    }
    return true;
  }
}
