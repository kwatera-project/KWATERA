package io.github.kwatera_project.kwatera.review_service.dto;

public record ReviewTranslationResponse(
    String translatedText,
    String targetLanguage,
    String sourceLanguage,
    boolean alreadyInTargetLanguage) {}
