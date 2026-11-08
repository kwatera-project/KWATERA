package io.github.kwatera_project.kwatera.review_service.controller;

import io.github.kwatera_project.kwatera.review_service.client.ReviewAiClient.SummaryResponse;
import io.github.kwatera_project.kwatera.review_service.model.ReviewSummary;
import io.github.kwatera_project.kwatera.review_service.repository.ReviewSummaryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/reviews")
@ApiResponse(responseCode = "200", description = "Cached AI review summary")
@ApiResponse(responseCode = "204", description = "No cached summary for this accommodation")
public class ReviewSummaryController {

  private final ReviewSummaryRepository summaries;
  private final ObjectMapper mapper;

  public ReviewSummaryController(ReviewSummaryRepository summaries, ObjectMapper mapper) {
    this.summaries = summaries;
    this.mapper = mapper;
  }

  @GetMapping("/units/{unitId}/summary")
  @Operation(summary = "Get the cached AI review summary for a unit")
  public ResponseEntity<SummaryResponse> forUnit(@PathVariable("unitId") UUID unitId) {
    return response(summaries.findByUnitId(unitId));
  }

  @GetMapping("/properties/{propertyId}/summary")
  @Operation(summary = "Get the cached AI review summary for a property")
  public ResponseEntity<SummaryResponse> forProperty(@PathVariable("propertyId") UUID propertyId) {
    return response(summaries.findByPropertyId(propertyId));
  }

  private ResponseEntity<SummaryResponse> response(Optional<ReviewSummary> cached) {
    if (cached.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(mapper.readValue(cached.get().getAiSummary(), SummaryResponse.class));
  }
}
