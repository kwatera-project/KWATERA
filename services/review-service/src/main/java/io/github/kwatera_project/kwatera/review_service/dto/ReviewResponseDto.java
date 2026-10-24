package io.github.kwatera_project.kwatera.review_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDto {
    private UUID id;
    private UUID reservationId;
    private UUID authorId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;
}