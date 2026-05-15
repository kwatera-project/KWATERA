package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.model.Settlement;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItem;
import java.util.List;

public record SettlementResponseDto(Settlement settlement, List<SettlementItem> items) {}
