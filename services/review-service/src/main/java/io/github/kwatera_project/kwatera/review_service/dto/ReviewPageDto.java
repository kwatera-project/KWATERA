package io.github.kwatera_project.kwatera.review_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReviewPageDto(
        List<PublicReviewDto> reviews,
        int page,
        int size,
        int totalPages,
        long reviewCount,
        BigDecimal averageRating) {}