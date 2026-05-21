package io.github.kwatera_project.kwatera.auth_service.dto;

import io.github.kwatera_project.kwatera.auth_service.model.Role;
import java.util.UUID;

public record UserProfileDto(
    UUID id, String username, String firstName, String lastName, String email, Role role) {}
