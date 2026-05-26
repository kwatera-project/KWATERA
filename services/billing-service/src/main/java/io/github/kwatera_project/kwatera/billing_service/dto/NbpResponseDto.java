package io.github.kwatera_project.kwatera.billing_service.dto;

import java.util.ArrayList;
import java.util.List;

public record NbpResponseDto(String table, String currency, String code, List<NbpRateDto> rates) {
  public NbpResponseDto {
    rates = rates != null ? new ArrayList<>(rates) : null;
  }

  @Override
  public List<NbpRateDto> rates() {
    return rates != null ? new ArrayList<>(rates) : null;
  }
}
