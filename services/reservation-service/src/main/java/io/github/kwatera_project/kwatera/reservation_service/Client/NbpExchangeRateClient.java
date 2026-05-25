package io.github.kwatera_project.kwatera.reservation_service.client;

import io.github.kwatera_project.kwatera.reservation_service.dto.NbpResponseDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NbpExchangeRateClient {

  private final RestTemplate restTemplate;

  public NbpExchangeRateClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Cacheable("exchangeRates")
  public NbpResponseDto getExchangeRate(String currencyCode) {
    String url = "http://api.nbp.pl/api/exchangerates/rates/a/" + currencyCode + "/?format=json";
    return restTemplate.getForObject(url, NbpResponseDto.class);
  }
}
