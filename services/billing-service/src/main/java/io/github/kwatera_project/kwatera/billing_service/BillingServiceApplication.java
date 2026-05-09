package io.github.kwatera_project.kwatera.billing_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class BillingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(BillingServiceApplication.class, args);
  }
}
