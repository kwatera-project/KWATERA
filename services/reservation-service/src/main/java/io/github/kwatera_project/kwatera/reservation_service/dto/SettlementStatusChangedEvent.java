package io.github.kwatera_project.kwatera.reservation_service.dto;

import io.github.kwatera_project.kwatera.reservation_service.model.SettlementStatus;

import java.util.UUID;

public record SettlementStatusChangedEvent(
        UUID reservationId,
        SettlementStatus settlementStatus
) {}