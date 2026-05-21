package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.math.BigDecimal;
import java.util.UUID;

public record FailedTransactionCommand(
    UUID settlementId,
    UUID unitId,
    SettlementItemType type,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice,
    String stripeSessionId,
    String failureReason) {}
