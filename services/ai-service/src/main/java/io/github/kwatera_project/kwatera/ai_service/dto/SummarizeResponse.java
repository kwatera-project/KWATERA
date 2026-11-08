package io.github.kwatera_project.kwatera.ai_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Generated review summary")
public record SummarizeResponse(
    @Schema(
            description = "Nonblank summary in the requested language",
            example = "Guests liked the clean room and friendly host, but reported unstable Wi-Fi.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String summary,
    @Schema(
            description = "Positive points supported by the reviews",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> pros,
    @Schema(
            description = "Negative points supported by the reviews",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> cons,
    @Schema(
            description = "Configured summary model tag",
            example = "qwen3:4b-instruct-2507-q4_K_M",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String model) {

  public SummarizeResponse {
    pros = List.copyOf(pros);
    cons = List.copyOf(cons);
  }
}
