package io.github.kwatera_project.kwatera.review_service.dto;

import java.util.UUID;

public record ReviewSummaryInput(UUID id, String comment) {}
