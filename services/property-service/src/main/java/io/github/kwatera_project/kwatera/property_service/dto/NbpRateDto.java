package io.github.kwatera_project.kwatera.property_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NbpRateDto(String no, LocalDate effectiveDate, BigDecimal mid) {}
