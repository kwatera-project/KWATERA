package io.github.kwatera_project.kwatera.billing_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing")
public class WebhookController {

  @PostMapping("/webhook")
  public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
    System.out.println("Webhook received: " + payload);
    return ResponseEntity.ok("success");
  }
}
