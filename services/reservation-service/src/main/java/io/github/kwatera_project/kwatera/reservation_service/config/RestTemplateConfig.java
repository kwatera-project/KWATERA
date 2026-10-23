package io.github.kwatera_project.kwatera.reservation_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/** Provides a load-balanced RestTemplate that resolves service names via Eureka. */
@Configuration
public class RestTemplateConfig {

  @Bean
  @LoadBalanced
  @SuppressWarnings("removal")
  public RestTemplate restTemplate(
      ObjectMapper objectMapper,
      @Value("${kwatera.http.connect-timeout:2s}") Duration connectTimeout,
      @Value("${kwatera.http.read-timeout:5s}") Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    RestTemplate restTemplate = new RestTemplate(requestFactory);
    MappingJackson2HttpMessageConverter converter =
        new MappingJackson2HttpMessageConverter(objectMapper);
    restTemplate
        .getMessageConverters()
        .removeIf(m -> m instanceof MappingJackson2HttpMessageConverter);
    restTemplate.getMessageConverters().add(converter);
    return restTemplate;
  }
}
