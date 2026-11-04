package io.github.kwatera_project.kwatera.ai_service.dto;

import io.github.kwatera_project.kwatera.ai_service.validation.LanguageTag;
import io.github.kwatera_project.kwatera.ai_service.validation.WithinAiLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WithinAiLimits
public record SummarizeRequest(
    @NotEmpty List<@NotBlank String> reviews, @NotBlank @LanguageTag String language) {
  public SummarizeRequest {
    // Retain null entries so Bean Validation can report them as a controlled 400.
    if (reviews != null) reviews = Collections.unmodifiableList(new ArrayList<>(reviews));
  }

  @Override
  public List<String> reviews() {
    return reviews == null ? null : Collections.unmodifiableList(reviews);
  }
}
