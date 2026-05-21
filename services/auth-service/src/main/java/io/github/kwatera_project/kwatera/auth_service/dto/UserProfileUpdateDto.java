package io.github.kwatera_project.kwatera.auth_service.dto;

import jakarta.validation.constraints.NotBlank;

public record UserProfileUpdateDto(@NotBlank String firstName, @NotBlank String lastName) {}
