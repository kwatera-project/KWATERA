package io.github.kwatera_project.kwatera.property_service.dto;

import io.github.kwatera_project.kwatera.property_service.model.BillingType;
import io.github.kwatera_project.kwatera.property_service.model.MeasurementUnit;
import io.github.kwatera_project.kwatera.property_service.model.SettlementItemType;
import java.math.BigDecimal;
import java.util.UUID;

public record UnitSettlementItemDto(
    UUID id,
    UUID unitId,
    SettlementItemType settlementItemType,
    BigDecimal pricePerUnit,
    MeasurementUnit measurementUnit,
    BillingType billingType) {}
