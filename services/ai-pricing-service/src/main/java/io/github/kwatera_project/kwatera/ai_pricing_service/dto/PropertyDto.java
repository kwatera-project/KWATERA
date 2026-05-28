package io.github.kwatera_project.kwatera.ai_pricing_service.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyDto {

  private UUID id;
  private String title;
  private String description;
  private String location;
  private String imageUrl;
  private BigDecimal latitude;
  private BigDecimal longitude;

  public PropertyDto(
      UUID id,
      String title,
      String description,
      String location,
      String imageUrl,
      BigDecimal latitude,
      BigDecimal longitude) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.location = location;
    this.imageUrl = imageUrl;
    this.latitude = latitude;
    this.longitude = longitude;
  }
}
