package io.github.kwatera_project.kwatera.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscribeRequest {

  @Email @NotBlank private String email;
}
