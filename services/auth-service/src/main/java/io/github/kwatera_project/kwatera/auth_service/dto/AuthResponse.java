package io.github.kwatera_project.kwatera.auth_service.dto;

import io.github.kwatera_project.kwatera.auth_service.model.Role;

/**
 * Response DTO from the authorisation layer. It deliberately does not contain passwords or other
 * sensitive data.
 */
public record AuthResponse(String username, Role role) {}
