package io.github.kwatera_project.kwatera.auth_service.dto;

import io.github.kwatera_project.kwatera.auth_service.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotNull
    private Role role;

    @NotBlank
    private String password;
}