package io.github.kwatera_project.kwatera.ai_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated review summary")
public record SummarizeResponse(
    @Schema(
            description = "Nonblank summary in the requested language",
            example = "Guests liked the clean room and friendly host, but reported unstable Wi-Fi.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String summary,
    @Schema(
            description = "Configured summary model tag",
            example = "qwen3:4b-instruct-2507-q4_K_M",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String model) {}
