package io.github.kwatera_project.kwatera.property_service.dto;

import io.github.kwatera_project.kwatera.property_service.model.UnitType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnitDto {

  private UUID id;
  private String name;
  private String description;
  private BigDecimal pricePerNight;
  private Integer capacity;
  private String imageUrl;
  private UUID propertyId;
  private UnitType unitType;
  private String unitNumber;
  private Integer floor;
  private BigDecimal convertedPricePerNight;
  private CurrencyMetadataDto currencyInfo;
}