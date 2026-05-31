package io.github.kwatera_project.kwatera.ai_pricing_service.dto;

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
public class PropertyDto {

  private UUID id;
  private String title;
  private String description;
  private String city;
  private String imageUrl;
  private BigDecimal latitude;
  private BigDecimal longitude;
}
