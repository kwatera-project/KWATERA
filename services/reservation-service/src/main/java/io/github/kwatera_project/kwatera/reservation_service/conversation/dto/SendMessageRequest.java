package io.github.kwatera_project.kwatera.reservation_service.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    @NotBlank
        @Size(max = 4000)
        @Schema(
            description =
                "Text stripped with Java String.strip() before validation; internal whitespace and line breaks are preserved",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 4000,
            example = "Hello, what time can we check in?")
        String content) {
  public SendMessageRequest {
    if (content != null) {
      content = content.strip();
    }
  }
}
