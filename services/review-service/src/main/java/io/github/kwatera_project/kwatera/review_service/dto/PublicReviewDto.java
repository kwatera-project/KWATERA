package io.github.kwatera_project.kwatera.review_service.dto;

import io.github.kwatera_project.kwatera.review_service.model.Review;
import java.time.Instant;
import java.util.UUID;

public record PublicReviewDto(
        UUID id,
        UUID unitId,
        Integer rating,
        String comment,
        Instant createdAt,
        Instant updatedAt) {

    public static PublicReviewDto from(Review review) {
        return new PublicReviewDto(
                review.getId(),
                review.getUnitId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}