package io.github.kwatera_project.kwatera.reservation_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Provides a load-balanced RestTemplate that resolves lb:// URIs via Eureka. */
@Configuration
public class RestTemplateConfig {

  @Bean
  @LoadBalanced
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
