package io.github.kwatera_project.kwatera.ai_service.error;

import io.github.kwatera_project.kwatera.ai_service.validation.LanguageTag;
import io.github.kwatera_project.kwatera.ai_service.validation.WithinAiLimits;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(AiException.class)
  public ResponseEntity<ApiError> ai(AiException exception) {
    return error(exception.code(), exception.getMessage(), exception.status().value());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
    var errors = exception.getBindingResult().getAllErrors();
    if (errors.stream().anyMatch(e -> WithinAiLimits.class.getSimpleName().equals(e.getCode()))) {
      return error("INPUT_TOO_LARGE", "Input exceeds configured limits.", 413);
    }
    if (errors.stream().anyMatch(e -> LanguageTag.class.getSimpleName().equals(e.getCode()))) {
      return error(
          "INVALID_LANGUAGE",
          "Use valid BCP 47 language tags; auto is allowed only for the source language.",
          400);
    }
    return error(
        "INVALID_REQUEST",
        "Required fields must be present and non-blank; reviews must contain non-blank entries.",
        400);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> malformed() {
    return error(
        "INVALID_REQUEST", "Request body must be valid JSON matching the API contract.", 400);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiError> mediaType() {
    return error("UNSUPPORTED_MEDIA_TYPE", "Use application/json.", 415);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> method() {
    return error("METHOD_NOT_ALLOWED", "HTTP method is not supported.", 405);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unexpected() {
    return error("INTERNAL_ERROR", "The request could not be processed.", 500);
  }

  private ResponseEntity<ApiError> error(String code, String message, int status) {
    return ResponseEntity.status(status).body(ApiError.of(code, message, status));
  }
}
