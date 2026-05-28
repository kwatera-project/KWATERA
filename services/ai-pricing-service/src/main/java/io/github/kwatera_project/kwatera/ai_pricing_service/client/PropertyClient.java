package io.github.kwatera_project.kwatera.ai_pricing_service.client;

import io.github.kwatera_project.kwatera.ai_pricing_service.dto.PropertyDto;
import io.github.kwatera_project.kwatera.ai_pricing_service.dto.UnitDto;
import io.github.kwatera_project.kwatera.ai_pricing_service.exception.PropertyNotFoundException;
import io.github.kwatera_project.kwatera.ai_pricing_service.exception.UnitNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PropertyClient {

  private final RestTemplate restTemplate;

  @Value("${services.property.url}")
  private String propertyServiceUrl;

  public UnitDto getUnit(UUID unitId) {

    String url = propertyServiceUrl + "/units/" + unitId;

    try {
      ResponseEntity<UnitDto> response = restTemplate.getForEntity(url, UnitDto.class);

      return response.getBody();

    } catch (HttpClientErrorException.NotFound e) {
      throw new UnitNotFoundException(unitId);
    }
  }

  public PropertyDto getProperty(UUID propertyId) {

    String url = propertyServiceUrl + "/" + propertyId;

    try {
      ResponseEntity<PropertyDto> response = restTemplate.getForEntity(url, PropertyDto.class);

      return response.getBody();

    } catch (HttpClientErrorException.NotFound e) {
      throw new PropertyNotFoundException(propertyId);
    }
  }
}
