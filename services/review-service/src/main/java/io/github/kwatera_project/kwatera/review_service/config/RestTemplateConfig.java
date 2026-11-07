package io.github.kwatera_project.kwatera.review_service.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

  @Bean
  @Primary
  @LoadBalanced
  public RestTemplate restTemplate(
      @Value("${kwatera.http.connect-timeout:2s}") Duration connectTimeout,
      @Value("${kwatera.http.read-timeout:5s}") Duration readTimeout) {
    return createRestTemplate(connectTimeout, readTimeout);
  }

  @Bean
  @LoadBalanced
  public RestTemplate reviewAiRestTemplate(
      @Value("${kwatera.http.connect-timeout:2s}") Duration connectTimeout,
      @Value("${kwatera.ai.read-timeout:130s}") Duration readTimeout) {
    return createRestTemplate(connectTimeout, readTimeout);
  }

  private RestTemplate createRestTemplate(Duration connectTimeout, Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    return new RestTemplate(requestFactory);
  }
}
