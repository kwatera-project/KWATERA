package io.github.kwatera_project.kwatera.billing_service.dto;

import java.math.BigDecimal;

public record OcrResponseDto(String readingValue, BigDecimal confidence) {}
