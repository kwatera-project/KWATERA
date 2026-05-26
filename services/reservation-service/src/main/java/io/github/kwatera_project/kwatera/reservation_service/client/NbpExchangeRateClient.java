package io.github.kwatera_project.kwatera.reservation_service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kwatera_project.kwatera.reservation_service.dto.NbpResponseDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class NbpExchangeRateClient {

  private final RestTemplate restTemplate;

  @SuppressWarnings("removal")
  public NbpExchangeRateClient(ObjectMapper objectMapper) {
    this.restTemplate = new RestTemplate();
    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(objectMapper);
    this.restTemplate
        .getMessageConverters()
        .removeIf(m -> m instanceof MappingJackson2HttpMessageConverter);
    this.restTemplate.getMessageConverters().add(converter);
  }

  @Cacheable("exchangeRates")
  public NbpResponseDto getExchangeRate(String currencyCode) {
    String url = "https://api.nbp.pl/api/exchangerates/rates/a/" + currencyCode + "/?format=json";
    return restTemplate.getForObject(url, NbpResponseDto.class);
  }
}
