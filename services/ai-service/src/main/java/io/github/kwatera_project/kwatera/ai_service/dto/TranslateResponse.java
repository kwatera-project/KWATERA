package io.github.kwatera_project.kwatera.ai_service.dto;

public record TranslateResponse(
    String translatedText, String sourceLanguage, String targetLanguage, String model) {}
