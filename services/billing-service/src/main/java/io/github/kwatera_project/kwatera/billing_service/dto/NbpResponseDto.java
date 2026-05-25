package io.github.kwatera_project.kwatera.billing_service.dto;

import java.util.List;

public record NbpResponseDto(String table, String currency, String code, List<NbpRateDto> rates) {}
