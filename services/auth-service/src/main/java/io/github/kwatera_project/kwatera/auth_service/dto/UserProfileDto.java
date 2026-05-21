package io.github.kwatera_project.kwatera.auth_service.dto;

import io.github.kwatera_project.kwatera.auth_service.model.Role;

public record UserProfileDto(
    String username, String firstName, String lastName, String email, Role role) {}
