package io.github.kwatera_project.kwatera.reservation_service.dto;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationOverviewDto(
    UUID id,
    String guestName,
    String unitName,
    LocalDate startDate,
    LocalDate endDate,
    ReservationStatus status) {}
