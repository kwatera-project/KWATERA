package io.github.kwatera_project.kwatera.property_service.dto;

import java.util.UUID;

public record OwnerPropertyCountDto(UUID ownerId, Long count) {}
