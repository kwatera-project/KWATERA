package io.github.kwatera_project.kwatera.property_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean
  public RestClient reservationRestClient(@Value("${services.reservation.url}") String baseUrl) {

    return RestClient.builder().baseUrl(baseUrl).build();
  }
}
