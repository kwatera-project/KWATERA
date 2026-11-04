package io.github.kwatera_project.kwatera.ai_service.dto;

import io.github.kwatera_project.kwatera.ai_service.validation.LanguageTag;
import io.github.kwatera_project.kwatera.ai_service.validation.WithinAiLimits;
import jakarta.validation.constraints.NotBlank;

@WithinAiLimits
public record TranslateRequest(
    @NotBlank String text,
    @LanguageTag(allowAuto = true) String sourceLanguage,
    @NotBlank @LanguageTag String targetLanguage) {}
