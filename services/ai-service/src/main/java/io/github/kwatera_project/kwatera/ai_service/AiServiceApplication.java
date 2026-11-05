package io.github.kwatera_project.kwatera.ai_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(AiServiceApplication.class, args);
  }
}
