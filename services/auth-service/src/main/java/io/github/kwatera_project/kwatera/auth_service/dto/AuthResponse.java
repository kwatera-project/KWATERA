package io.github.kwatera_project.kwatera.auth_service.dto;

import lombok.Getter;

@Getter
public class AuthResponse {
  private String token;

  public AuthResponse(String token) {
    this.token = token;
  }
}
