package io.github.kwatera_project.kwatera.reservation_service.client;

import io.github.kwatera_project.kwatera.reservation_service.dto.NbpResponseDto;
import java.net.URI;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NbpExchangeRateClient {

  private static final URI EUR_RATE_URI =
      URI.create("https://api.nbp.pl/api/exchangerates/rates/a/EUR/?format=json");

  private static final URI USD_RATE_URI =
      URI.create("https://api.nbp.pl/api/exchangerates/rates/a/USD/?format=json");

  private final RestTemplate restTemplate;

  public NbpExchangeRateClient() {
    this.restTemplate = new RestTemplate();
  }

  @Cacheable("exchangeRatesEur")
  public NbpResponseDto getEurExchangeRate() {
    return restTemplate.getForObject(EUR_RATE_URI, NbpResponseDto.class);
  }

  @Cacheable("exchangeRatesUsd")
  public NbpResponseDto getUsdExchangeRate() {
    return restTemplate.getForObject(USD_RATE_URI, NbpResponseDto.class);
  }
}
