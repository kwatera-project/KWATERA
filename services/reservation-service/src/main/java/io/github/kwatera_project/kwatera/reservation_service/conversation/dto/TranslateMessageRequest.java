package io.github.kwatera_project.kwatera.reservation_service.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TranslateMessageRequest(@NotBlank @Size(max = 64) String targetLanguage) {}
