package io.github.kwatera_project.kwatera.ai_service.dto;

import io.github.kwatera_project.kwatera.ai_service.validation.LanguageTag;
import io.github.kwatera_project.kwatera.ai_service.validation.WithinAiLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@WithinAiLimits
@Schema(description = "Message translation request with optional source-language detection")
public record TranslateRequest(
    @Schema(
            description = "Nonblank original message; character limit is configurable",
            example = "Hello, what time can I check in?",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String text,
    @Schema(
            description = "Source BCP 47 tag; omit, use null, or auto to request detection",
            example = "auto",
            types = {"string", "null"},
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @LanguageTag(allowAuto = true)
        String sourceLanguage,
    @Schema(
            description =
                "Target BCP 47 tag, including regional/script variants; auto is not allowed",
            example = "pl",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @LanguageTag
        String targetLanguage) {}
