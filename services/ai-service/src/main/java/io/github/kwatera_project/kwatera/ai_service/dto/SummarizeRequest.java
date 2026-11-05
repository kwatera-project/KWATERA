package io.github.kwatera_project.kwatera.ai_service.dto;

import io.github.kwatera_project.kwatera.ai_service.validation.LanguageTag;
import io.github.kwatera_project.kwatera.ai_service.validation.WithinAiLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@WithinAiLimits
@Schema(
    description =
        "Reviews to summarize; count, per-review and total character limits are configurable")
public record SummarizeRequest(
    @Schema(
            description = "Nonempty list of nonblank reviews",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[\"Clean room and friendly host.\",\"Wi-Fi was unstable.\"]")
        @NotEmpty
        List<@NotBlank String> reviews,
    @Schema(
            description = "Target BCP 47 language tag; auto is not allowed",
            example = "en",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @LanguageTag
        String language) {
  public SummarizeRequest {
    // Retain null entries so Bean Validation can report them as a controlled 400.
    if (reviews != null) reviews = Collections.unmodifiableList(new ArrayList<>(reviews));
  }

  @Override
  public List<String> reviews() {
    return reviews == null ? null : Collections.unmodifiableList(reviews);
  }
}
