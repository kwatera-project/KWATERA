package io.github.kwatera_project.kwatera.review_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationDetailsDto(
        UUID id,
        UUID userId,
        UUID unitId,
        String status,
        LocalDate endDate
) {}