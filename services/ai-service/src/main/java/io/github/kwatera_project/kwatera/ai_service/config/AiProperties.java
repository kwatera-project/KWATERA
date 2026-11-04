package io.github.kwatera_project.kwatera.ai_service.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kwatera.ai")
public record AiProperties(
    @NotBlank String baseUrl,
    @NotBlank String model,
    @NotNull @DurationMin(millis = 1) Duration connectTimeout,
    @NotNull @DurationMin(millis = 1) Duration requestTimeout,
    @NotNull @DurationMin(millis = 1) Duration readinessTimeout,
    @DecimalMin("0.0") @DecimalMax("2.0") double temperature,
    @Positive int maxOutputTokens,
    @Positive int contextTokens,
    @Positive int maxReviews,
    @Positive int maxReviewCharacters,
    @Positive int maxTotalReviewCharacters,
    @Positive int maxTranslationCharacters,
    @Positive int maxResponseCharacters) {}
