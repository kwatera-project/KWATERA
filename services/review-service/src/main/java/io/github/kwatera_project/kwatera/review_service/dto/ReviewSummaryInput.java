package io.github.kwatera_project.kwatera.review_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewSummaryInput(UUID id, String comment, Instant updatedAt) {}
