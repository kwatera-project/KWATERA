package io.github.kwatera_project.kwatera.reservation_service.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateRangeDtoTest {

  @Test
  void allArgsConstructorAndGetters() {
    LocalDate start = LocalDate.of(2026, 6, 1);
    LocalDate end = LocalDate.of(2026, 6, 7);

    DateRangeDto dto = new DateRangeDto(start, end);

    assertEquals(start, dto.getStartDate());
    assertEquals(end, dto.getEndDate());
  }

  @Test
  void noArgsConstructorAndSetters() {
    LocalDate start = LocalDate.of(2026, 8, 10);
    LocalDate end = LocalDate.of(2026, 8, 15);

    DateRangeDto dto = new DateRangeDto();
    dto.setStartDate(start);
    dto.setEndDate(end);

    assertEquals(start, dto.getStartDate());
    assertEquals(end, dto.getEndDate());
  }
}
