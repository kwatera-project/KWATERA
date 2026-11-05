package io.github.kwatera_project.kwatera.ai_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Translated message, or unchanged input for exactly matching canonical languages")
public record TranslateResponse(
    @Schema(
            description = "Translation text; model wording can vary",
            example = "Dzień dobry, o której godzinie mogę się zameldować?",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String translatedText,
    @Schema(
            description =
                "Canonical requested source tag, or auto for detection mode; not a claimed detected language",
            example = "auto",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String sourceLanguage,
    @Schema(
            description = "Canonical target BCP 47 tag",
            example = "pl",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String targetLanguage,
    @Schema(
            description = "Configured translation model tag, including on the same-language bypass",
            example = "translategemma:4b",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String model) {}
