package io.github.kwatera_project.kwatera.billing_service.dto;

import io.github.kwatera_project.kwatera.billing_service.exception.WebhookProcessingException;
import io.github.kwatera_project.kwatera.billing_service.model.SettlementItemType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record PaymentMetadataDto(
    UUID settlementId,
    UUID unitId,
    SettlementItemType type,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice) {

  public static PaymentMetadataDto from(Map<String, String> metadata) {

    if (metadata == null) {
      throw new WebhookProcessingException("Metadata is missing entirely");
    }

    String settlementIdStr = metadata.get("settlementId");
    if (settlementIdStr == null || settlementIdStr.isBlank()) {
      throw new WebhookProcessingException("Missing settlementId in metadata");
    }

    String unitIdStr = metadata.get("unitId");
    if (unitIdStr == null || unitIdStr.isBlank()) {
      throw new WebhookProcessingException("Missing unitId in metadata");
    }

    String typeStr = metadata.get("type");
    if (typeStr == null || typeStr.isBlank()) {
      throw new WebhookProcessingException("Missing type in metadata");
    }

    String descriptionStr = metadata.get("description");
    if (descriptionStr == null || descriptionStr.isBlank()) {
      throw new WebhookProcessingException("Missing description in metadata");
    }

    String quantityStr = metadata.get("quantity");
    if (quantityStr == null || quantityStr.isBlank()) {
      throw new WebhookProcessingException("Missing quantity in metadata");
    }

    String unitPriceStr = metadata.get("unitPrice");
    if (unitPriceStr == null || unitPriceStr.isBlank()) {
      throw new WebhookProcessingException("Missing unitPrice in metadata");
    }

    try {
      return new PaymentMetadataDto(
          UUID.fromString(settlementIdStr),
          UUID.fromString(unitIdStr),
          SettlementItemType.valueOf(typeStr),
          descriptionStr,
          parseBigDecimal(quantityStr),
          parseBigDecimal(unitPriceStr));

    } catch (Exception e) {
      throw new WebhookProcessingException("Invalid payment metadata", e);
    }
  }

  private static BigDecimal parseBigDecimal(String val) {

    try {
      return val != null ? new BigDecimal(val) : BigDecimal.ZERO;

    } catch (NumberFormatException e) {
      throw new WebhookProcessingException("Invalid decimal: " + val);
    }
  }
}
