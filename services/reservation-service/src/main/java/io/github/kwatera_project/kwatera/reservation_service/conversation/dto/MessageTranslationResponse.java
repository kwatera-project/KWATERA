package io.github.kwatera_project.kwatera.reservation_service.conversation.dto;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public record MessageTranslationResponse(
    String translatedText, String sourceLanguage, String targetLanguage) {}
