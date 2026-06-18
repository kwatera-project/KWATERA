package io.github.kwatera_project.kwatera.auth_service.dto;

import io.github.kwatera_project.kwatera.auth_service.model.Role;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponseDto(
    UUID id,
    String firstName,
    String lastName,
    String email,
    Role role,
    String status,
    Instant createdAt,
    long propertyCount) {}
