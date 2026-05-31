package io.github.kwatera_project.kwatera.property_service.dto;

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
  private String country;
  private String postalCode;
  private String street;
  private String streetNumber;
}
