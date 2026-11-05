package io.github.kwatera_project.kwatera.ai_service.controller;

import io.github.kwatera_project.kwatera.ai_service.config.OpenApiConfig;
import io.github.kwatera_project.kwatera.ai_service.dto.*;
import io.github.kwatera_project.kwatera.ai_service.error.ApiError;
import io.github.kwatera_project.kwatera.ai_service.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/ai")
@Tag(name = "Internal AI", description = "Optional AI operations for trusted backend callers")
@SecurityRequirement(name = OpenApiConfig.INTERNAL_TOKEN)
@ApiResponse(
    responseCode = "400",
    description =
        "INVALID_REQUEST: malformed JSON or missing/blank fields; INVALID_LANGUAGE: invalid BCP 47 tag",
    content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
@ApiResponse(
    responseCode = "403",
    description = "ACCESS_DENIED: missing or invalid X-Internal-Token",
    content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
@ApiResponse(
    responseCode = "405",
    description = "METHOD_NOT_ALLOWED: unsupported HTTP method",
    content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
@ApiResponse(
    responseCode = "413",
    description = "INPUT_TOO_LARGE: configured input limits exceeded",
    content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
@ApiResponse(
    responseCode = "415",
    description = "UNSUPPORTED_MEDIA_TYPE: application/json required",
    content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
@ApiResponse(
    responseCode = "500",
    description = "INTERNAL_ERROR: unexpected application processing error",
    content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
@ApiResponse(
    responseCode = "502",
    description =
        "AI_INVALID_RESPONSE: invalid, empty or truncated output, or unusable source detection",
    content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
@ApiResponse(
    responseCode = "503",
    description =
        "AI_UNAVAILABLE: backend unavailable; AI_MODEL_NOT_READY: configured model missing",
    content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
@ApiResponse(
    responseCode = "504",
    description = "AI_TIMEOUT: backend exceeded a configured timeout",
    content =
        @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
public class AiController {
  private final AiService service;

  public AiController(AiService service) {
    this.service = service;
  }

  @PostMapping("/summarize")
  @Operation(
      summary = "Summarize customer reviews",
      description =
          "Create a concise summary in the requested BCP 47 language. Uses the independently configured summary model. Reviews must be nonempty; count and character limits are configurable.")
  @ApiResponse(
      responseCode = "200",
      description = "Generated summary and configured summary model",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SummarizeResponse.class)))
  public SummarizeResponse summarize(@Valid @RequestBody SummarizeRequest request) {
    return service.summarize(request);
  }

  @PostMapping("/translate")
  @Operation(
      summary = "Translate a booking message",
      description =
          "Translate to the requested BCP 47 language using the translation model. Omitted/null/auto source requests language detection; the response reports auto, not a claimed detected tag. Exactly matching canonical source/target tags return unchanged text without inference. Regional tags remain distinct. Input/output limits and backend timeouts apply; TranslateGemma auto mode can make two sequential backend calls.")
  @ApiResponse(
      responseCode = "200",
      description = "Translation or unchanged same-language text and configured translation model",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TranslateResponse.class)))
  public TranslateResponse translate(@Valid @RequestBody TranslateRequest request) {
    return service.translate(request);
  }
}
