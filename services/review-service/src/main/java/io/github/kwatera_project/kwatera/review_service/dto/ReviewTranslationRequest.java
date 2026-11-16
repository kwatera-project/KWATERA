package io.github.kwatera_project.kwatera.review_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReviewTranslationRequest(
    @NotBlank @Pattern(regexp = "^(en|pl)$") String targetLanguage) {}
