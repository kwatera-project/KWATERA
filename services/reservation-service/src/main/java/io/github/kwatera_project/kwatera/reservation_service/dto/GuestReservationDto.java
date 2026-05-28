package io.github.kwatera_project.kwatera.reservation_service.dto;

import io.github.kwatera_project.kwatera.reservation_service.model.ReservationStatus;
import java.time.LocalDate;
import java.util.UUID;

public record GuestReservationDto(
    UUID id,
    UUID unitId,
    LocalDate startDate,
    LocalDate endDate,
    ReservationStatus status,
    java.math.BigDecimal totalPrice,
    java.math.BigDecimal convertedTotalPrice,
    CurrencyMetadataDto currencyInfo) {}
