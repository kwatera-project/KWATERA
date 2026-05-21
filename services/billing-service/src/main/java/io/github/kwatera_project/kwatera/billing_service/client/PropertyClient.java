package io.github.kwatera_project.kwatera.billing_service.client;

import io.github.kwatera_project.kwatera.billing_service.dto.UnitSettlementItemDto;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class PropertyClient {

  private final RestTemplate restTemplate;

  @Value("${services.property.url}")
  private String propertyServiceUrl;

  public List<UnitSettlementItemDto> getUnitSettlementItems(UUID unitId) {

    String url = propertyServiceUrl + "/units/" + unitId + "/settlement-items";

    ResponseEntity<UnitSettlementItemDto[]> response =
        restTemplate.getForEntity(url, UnitSettlementItemDto[].class);

    return Arrays.asList(Objects.requireNonNull(response.getBody()));
  }
}
