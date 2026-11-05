package io.github.kwatera_project.kwatera.reservation_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class TranslationClientConfig {
  @Bean
  @LoadBalanced
  @SuppressWarnings("removal")
  public RestClient.Builder translationRestClientBuilder(
      ObjectMapper mapper,
      @Value("${kwatera.ai.connect-timeout:2s}") Duration connectTimeout,
      @Value("${kwatera.ai.read-timeout:250s}") Duration readTimeout) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(readTimeout);
    return RestClient.builder()
        .requestFactory(factory)
        .messageConverters(
            converters -> {
              converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
              converters.add(0, new MappingJackson2HttpMessageConverter(mapper));
            });
  }
}
