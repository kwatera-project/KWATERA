package io.github.kwatera_project.kwatera.property_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class UnitDto {

  private UUID id;
  private String name;
  private String description;
  private BigDecimal pricePerNight;
  private Integer capacity;
  private String imageUrl;

  private BigDecimal convertedPricePerNight;
  private io.github.kwatera_project.kwatera.property_service.dto.CurrencyMetadataDto currencyInfo;

  public UnitDto(
      UUID id,
      String name,
      String description,
      BigDecimal pricePerNight,
      Integer capacity,
      String imageUrl,
      BigDecimal convertedPricePerNight,
      io.github.kwatera_project.kwatera.property_service.dto.CurrencyMetadataDto currencyInfo) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.pricePerNight = pricePerNight;
    this.capacity = capacity;
    this.imageUrl = imageUrl;
    this.convertedPricePerNight = convertedPricePerNight;
    this.currencyInfo = currencyInfo;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getPricePerNight() {
    return pricePerNight;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public BigDecimal getConvertedPricePerNight() {
    return convertedPricePerNight;
  }

  public io.github.kwatera_project.kwatera.property_service.dto.CurrencyMetadataDto getCurrencyInfo() {
    return currencyInfo;
  }
}
